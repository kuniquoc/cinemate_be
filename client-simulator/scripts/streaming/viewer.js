#!/usr/bin/env node

const path = require('node:path');
const fs = require('fs-extra');
const axios = require('axios');
const crypto = require('node:crypto');
const config = require('../../config/config');
const { log, delay, createApiClient, handleApiError } = require('../utils');
const SignalingClient = require('./signaling-client');
const PlaybackBuffer = require('./playback-buffer');
const { scorePeers } = require('./peer-selector');
// moved WebRTC details into separate modules
const WebRtcConnectionManager = require('./webrtc/connection-manager');
const {
    DEFAULT_CHUNK_SIZE
} = require('./webrtc/chunk-protocol');

const segmentCache = new Map();
const DEFAULT_SEEDER_TEMPLATE = '/streams/{streamId}/segments/{segmentId}';

function formatAxiosError(error, url) {
    if (axios?.isAxiosError?.(error)) {
        if (error.response) {
            const status = error.response.status;
            const statusText = error.response.statusText ?? 'Unknown';
            return `HTTP ${status} ${statusText} at ${url}`;
        }
        if (error.request) {
            const code = error.code ?? 'NO_RESPONSE';
            return `No response from ${url} (code=${code}, message=${error.message ?? 'n/a'})`;
        }
        return error.message || 'Unexpected Axios error';
    }
    if (error instanceof Error) {
        return error.message || error.name || 'Unknown error';
    }
    return String(error);
}

function parseStreamDescriptor(streamId) {
    if (!streamId) {
        return null;
    }
    const trimmed = String(streamId).trim();
    const separator = trimmed.indexOf('_');
    if (separator <= 0 || separator === trimmed.length - 1) {
        return { streamId: trimmed }; // allow legacy identifiers
    }
    const movieId = trimmed.slice(0, separator);
    const quality = trimmed.slice(separator + 1);
    if (!movieId || !quality) {
        return { streamId: trimmed };
    }
    return {
        streamId: trimmed,
        movieId,
        quality
    };
}

function parseArgs(argv) {
    const args = {};
    for (let i = 2; i < argv.length; i++) {
        const token = argv[i];
        if (!token.startsWith('--')) {
            continue;
        }
        const [key, value] = token.slice(2).split('=');
        args[key] = value === undefined ? true : value;
    }
    return args;
}

async function loadPlaylist(args, playlistCfg, streamDescriptor, streamingCfg) {
    if (args.manifest) {
        return readManifest(args.manifest);
    }
    if (playlistCfg.manifestPath) {
        const manifestSegments = await readManifest(playlistCfg.manifestPath);
        if (manifestSegments.length > 0) {
            return manifestSegments;
        }
        log.warn(`Manifest file at ${playlistCfg.manifestPath} is empty, falling back to movie detail.`);
    }
    const manifestSegments = await loadSegmentsFromMovieInfo(streamDescriptor, streamingCfg);
    if (manifestSegments.length === 0) {
        throw new Error('No valid manifest found for the requested stream.');
    }
    return manifestSegments;
}

async function readManifest(filePath) {
    const content = await fs.readFile(filePath, 'utf8');
    return content
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0 && !line.startsWith('#'));
}

async function loadSegmentsFromMovieInfo(streamDescriptor, streamingCfg) {
    if (!streamDescriptor?.movieId || !streamDescriptor?.quality) {
        return [];
    }
    let manifestUrl = null;
    let qualityKey = null;
    try {
        const resolved = await resolveQualityManifest(streamDescriptor, streamingCfg);
        qualityKey = resolved.qualityKey;
        manifestUrl = resolved.manifestUrl;
        if (!manifestUrl) {
            return [];
        }
        const response = await axios.get(manifestUrl, {
            responseType: 'text',
            timeout: streamingCfg.playback?.fallbackHttpTimeoutMs ?? 5000
        });
        const segments = parseManifestSegments(response.data);
        if (segments.length > 0) {
            log.info(`Loaded manifest ${qualityKey} with ${segments.length} segments from ${manifestUrl}`);
        }
        return segments;
    } catch (error) {
        if (error.response) {
            log.warn(`Failed to download manifest ${manifestUrl ?? ''} (HTTP ${error.response.status})`);
        } else if (error.request) {
            log.warn(`Unable to reach ${manifestUrl ?? 'manifest'}: ${error.message}`);
        } else {
            log.warn(`Failed to load manifest: ${error.message}`);
        }
        return [];
    }
}

async function resolveQualityManifest(streamDescriptor, streamingCfg) {
    const apiClient = createApiClient();
    try {
        const response = await apiClient.get(`/api/movies/${streamDescriptor.movieId}`);
        const payload = response?.data?.data;
        const qualities = payload?.qualities;
        if (!qualities || typeof qualities !== 'object') {
            log.warn(`Movie ${streamDescriptor.movieId} does not provide quality info in API response.`);
            return { qualityKey: null, manifestUrl: null };
        }
        const qualityEntry = pickQualityEntry(qualities, streamDescriptor.quality);
        if (!qualityEntry) {
            log.warn(`Quality "${streamDescriptor.quality}" missing in API response.`);
            return { qualityKey: null, manifestUrl: null };
        }
        const manifestUrl = resolveManifestUrl(qualityEntry.url, streamingCfg);
        if (!manifestUrl) {
            log.warn(`Unable to construct manifest URL for quality ${qualityEntry.key}.`);
            return { qualityKey: null, manifestUrl: null };
        }
        return { qualityKey: qualityEntry.key, manifestUrl };
    } catch (error) {
        handleApiError(error, 'Call movie info API');
        return { qualityKey: null, manifestUrl: null };
    }
}

function pickQualityEntry(qualities, desiredQuality) {
    if (!qualities) {
        return null;
    }
    if (qualities[desiredQuality]) {
        return { key: desiredQuality, url: qualities[desiredQuality] };
    }
    const lowerDesired = String(desiredQuality).toLowerCase();
    const matchedKey = Object.keys(qualities).find((key) => key.toLowerCase() === lowerDesired);
    if (matchedKey) {
        return { key: matchedKey, url: qualities[matchedKey] };
    }
    return null;
}

function resolveManifestUrl(rawUrl, streamingCfg) {
    if (!rawUrl) {
        return null;
    }
    if (/^https?:\/\//i.test(rawUrl)) {
        return rawUrl;
    }
    const originBase = streamingCfg?.fallback?.origin?.baseUrl;
    if (!originBase) {
        return rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`;
    }
    const baseClean = originBase.replaceAll(/\/+$/g, '');
    const pathClean = rawUrl.replaceAll(/^\/+/g, '');
    const baseSegments = baseClean.split('/');
    const pathSegments = pathClean.split('/');
    const baseLast = baseSegments[baseSegments.length - 1];
    if (pathSegments.length > 0 && baseLast === pathSegments[0]) {
        pathSegments.shift();
    }
    return `${baseClean}/${pathSegments.join('/')}`;
}

function parseManifestSegments(manifestContent) {
    if (!manifestContent) {
        return [];
    }
    const seen = new Set();
    const result = [];
    const lines = manifestContent.split(/\r?\n/);
    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line || line.startsWith('#')) {
            continue;
        }
        const withoutQuery = line.split('?')[0];
        const parts = withoutQuery.split('/');
        const filename = parts.pop();
        if (!filename) {
            continue;
        }
        const dotIndex = filename.indexOf('.');
        const segmentId = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        if (!segmentId || seen.has(segmentId)) {
            continue;
        }
        seen.add(segmentId);
        result.push(segmentId);
    }
    return result;
}

function randomBetween(min, max) {
    const realMin = Math.min(min, max);
    const realMax = Math.max(min, max);
    return realMin + Math.random() * (realMax - realMin);
}

function buildSegmentUrl(baseUrl, template, streamDescriptor, segmentId) {
    if (!baseUrl) {
        return null;
    }
    const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    const descriptor = streamDescriptor ?? { streamId: '' };
    const pathTemplate = (template || DEFAULT_SEEDER_TEMPLATE)
        .replaceAll('{streamId}', encodeURIComponent(descriptor?.streamId ?? ''))
        .replaceAll('{segmentId}', encodeURIComponent(segmentId));
    const resolved = pathTemplate
        .replaceAll('{movieId}', encodeURIComponent(descriptor?.movieId ?? ''))
        .replaceAll('{quality}', encodeURIComponent(descriptor?.quality ?? ''));
    return `${normalizedBase}${resolved.startsWith('/') ? '' : '/'}${resolved}`;
}

function cacheKey(streamId, segmentId) {
    return `${streamId}:${segmentId}`;
}

function computeSpeedMbps(bytes, latencyMs) {
    if (!bytes || latencyMs <= 0) {
        return 0;
    }
    const seconds = latencyMs / 1000;
    const megabytes = bytes / (1024 * 1024);
    return megabytes / seconds;
}


async function ensureCacheDirectory(fallbackCfg, streamId) {
    if (!fallbackCfg.persistCacheToDisk) {
        return;
    }
    const directory = path.resolve(fallbackCfg.cacheDirectory, streamId);
    await fs.ensureDir(directory);
}

function getCachedSegment(ctx, segmentId) {
    const entry = segmentCache.get(cacheKey(ctx.streamId, segmentId));
    if (!entry) {
        return null;
    }
    return {
        ...entry,
        data: Buffer.from(entry.data),
        fromCache: true
    };
}

async function cacheSegmentLocally(ctx, record) {
    const key = cacheKey(ctx.streamId, record.segmentId);
    if (!segmentCache.has(key)) {
        segmentCache.set(key, {
            streamId: ctx.streamId,
            segmentId: record.segmentId,
            data: Buffer.from(record.data),
            sizeBytes: record.sizeBytes,
            source: record.source,
            provider: record.provider,
            latencyMs: record.latencyMs,
            speedMbps: record.speedMbps,
            cachedAt: Date.now()
        });
        if (ctx.fallbackCfg.persistCacheToDisk) {
            const directory = path.resolve(ctx.fallbackCfg.cacheDirectory, ctx.streamId);
            const filename = `${record.segmentId}.bin`;
            const filePath = path.join(directory, filename);
            await fs.ensureDir(directory);
            await fs.writeFile(filePath, record.data);
        }
    }
}

async function tryPeerConnection(ctx, peer, segmentId) {
    const peerConfig = ctx.peersCfg?.[peer.peerId];
    if (!peerConfig) {
        return { success: false, error: 'No endpoint configured for peer' };
    }
    const url = buildSegmentUrl(
        peerConfig.baseUrl,
        peerConfig.segmentPathTemplate || ctx.templates.seeder,
        ctx.streamDescriptor,
        segmentId
    );
    if (!url) {
        return { success: false, error: 'Peer URL could not be built' };
    }
    const started = Date.now();
    try {
        const response = await axios.get(url, {
            responseType: 'arraybuffer',
            timeout: ctx.playbackCfg.peerConnectTimeoutMs
        });
        const latencyMs = Date.now() - started;
        const dataBuffer = Buffer.from(response.data);
        const speedMbps = computeSpeedMbps(dataBuffer.length, latencyMs);
        return {
            success: true,
            data: dataBuffer,
            latencyMs,
            speedMbps,
            provider: peer.peerId,
            reportSource: 'peer',
            url
        };
    } catch (error) {
        const message = error.response ? `HTTP ${error.response.status}` : error.message;
        return { success: false, error: message };
    }
}

function resolveSeederEndpoint(ctx) {
    const baseUrl = ctx.fallbackCfg.seeder?.baseUrl;
    if (!baseUrl) {
        return null;
    }
    return {
        name: 'seeder',
        baseUrl,
        template: ctx.templates.seeder
    };
}

async function fetchFromSeeder(ctx, segmentId) {
    const endpoint = resolveSeederEndpoint(ctx);
    if (!endpoint) {
        throw new Error('Seeder fallback is not configured. Set STREAMING_SEEDER_BASE_URL or config.streaming.fallback.seeder.baseUrl.');
    }
    const url = buildSegmentUrl(endpoint.baseUrl, endpoint.template, ctx.streamDescriptor, segmentId);
    if (!url) {
        throw new Error('Seeder URL could not be built.');
    }
    const started = Date.now();
    try {
        const response = await axios.get(url, {
            responseType: 'arraybuffer',
            timeout: ctx.playbackCfg.fallbackHttpTimeoutMs
        });
        const latencyMs = Date.now() - started;
        const dataBuffer = Buffer.from(response.data);
        const speedMbps = computeSpeedMbps(dataBuffer.length, latencyMs);
        return {
            data: dataBuffer,
            latencyMs,
            speedMbps,
            provider: endpoint.name,
            reportSource: 'seeder',
            url
        };
    } catch (error) {
        const detail = formatAxiosError(error, url);
        throw new Error(`Seeder fetch failed: ${detail}`, { cause: error });
    }
}

async function fetchSegment(segmentId, ctx, options = {}) {
    const { forceFallback = false, reason = null } = options;
    const fallbackTag = forceFallback ? ` (force seeder${reason ? `: ${reason}` : ''})` : '';
    const cached = getCachedSegment(ctx, segmentId);
    if (cached) {
        log.success(`Cache hit for segment ${segmentId}`);
        return cached;
    }

    log.info(`Viewer requesting segment ${segmentId} of stream ${ctx.streamId}${fallbackTag}`);
    let peers = [];
    if (!forceFallback) {
        try {
            peers = await ctx.signaling.requestWhoHas(segmentId, ctx.playbackCfg.whoHasQueryTimeoutMs);
            if (Array.isArray(peers) && peers.length === 0) {
                log.warn(`No peers currently advertise segment ${segmentId}.`);
            }
        } catch (error) {
            log.warn(`WHO_HAS failed for segment ${segmentId}: ${error.message}`);
        }
    } else {
        const reasonNote = reason ? ` (reason: ${reason})` : '';
        log.info(`Skip peer discovery for segment ${segmentId}${reasonNote} because seeder fetch is forced.`);
    }

    let result = null;
    if (!forceFallback && Array.isArray(peers) && peers.length > 0) {
        const scoredPeers = scorePeers(peers, ctx.scoringCfg);
        const maxPeers = Math.max(1, ctx.playbackCfg.maxActivePeers);
        for (const peer of scoredPeers.slice(0, maxPeers)) {
            const waitMs = randomBetween(ctx.playbackCfg.segmentRequestWaitMinMs, ctx.playbackCfg.segmentRequestWaitMaxMs);
            await delay(waitMs);
            log.info(`Trying peer ${peer.peerId} for segment ${segmentId} (score=${peer.score.toFixed(3)})`);
            const attempt = await tryPeerConnection(ctx, peer, segmentId);
            if (attempt.success) {
                log.success(`Received segment ${segmentId} from peer ${peer.provider}`);
                result = attempt;
                break;
            } else {
                log.warn(`Peer ${peer.peerId} failed to deliver segment ${segmentId}: ${attempt.error}`);
            }
        }
    }

    if (!result) {
        if (forceFallback) {
            const reasonNote = reason ? ` (reason: ${reason})` : '';
            log.info(`Requesting segment ${segmentId} from seeder${reasonNote}.`);
        } else {
            log.warn(`All peer attempts failed for segment ${segmentId} - fallback to seeder`);
        }
        result = await fetchFromSeeder(ctx, segmentId);
    }

    const record = {
        streamId: ctx.streamId,
        segmentId,
        data: result.data,
        sizeBytes: result.data.length,
        latencyMs: result.latencyMs,
        speedMbps: result.speedMbps,
        source: result.reportSource,
        provider: result.provider,
        originUrl: result.url,
        receivedAt: Date.now()
    };

    await cacheSegmentLocally(ctx, record);
    await ctx.signaling.reportSegment({
        segmentId,
        source: record.source,
        latencyMs: record.latencyMs,
        speedMbps: record.speedMbps
    });

    return record;
}

async function main() {
    const args = parseArgs(process.argv);
    if (!config.streaming) {
        log.error('Streaming simulator configuration not found in config/config.js');
        process.exit(1);
    }

    const streamingCfg = config.streaming;
    const streamId = args.stream || streamingCfg.defaultStreamId;
    if (!streamId) {
        log.error('Missing streamId. Provide --stream=<movieId>_<quality> or set STREAMING_DEFAULT_STREAM_ID.');
        process.exit(1);
    }
    const streamDescriptor = parseStreamDescriptor(streamId);
    if (!streamDescriptor.movieId || !streamDescriptor.quality) {
        log.warn('streamId "%s" does not follow <movieId>_<quality> format. Seeder still works, manifest lookup may be limited.', streamDescriptor.streamId);
    }
    const clientId = args.client || `${streamingCfg.defaultClientPrefix}${crypto.randomUUID()}`;
    let playlist;
    try {
        playlist = await loadPlaylist(args, streamingCfg.playlist, streamDescriptor, streamingCfg);
    } catch (error) {
        log.error(`Unable to load playlist: ${error.message}`);
        process.exit(1);
    }

    if (!playlist.length) {
        log.error('Playlist is empty. Provide a manifest file.');
        process.exit(1);
    }

    const playbackCfg = streamingCfg.playback;
    const scoringCfg = streamingCfg.scoring;
    const fallbackCfgSource = streamingCfg.fallback || {};
    const fallbackCfg = {
        persistCacheToDisk: fallbackCfgSource.persistCacheToDisk ?? true,
        cacheDirectory: fallbackCfgSource.cacheDirectory || './.cache/streaming',
        seeder: fallbackCfgSource.seeder || {},
        origin: fallbackCfgSource.origin || {}
    };
    const seederTemplate = fallbackCfg.seeder.segmentPathTemplate || DEFAULT_SEEDER_TEMPLATE;

    const signaling = new SignalingClient({
        url: streamingCfg.signaling.wsBaseUrl,
        clientId,
        streamId,
        logger: log
    });

    // Initialize WebRTC Connection Manager
    const webrtcManager = new WebRtcConnectionManager({
        clientId,
        streamId,
        signaling,
        stunServers: streamingCfg.webrtc?.stunServers,
        logger: log,
        chunkSize: DEFAULT_CHUNK_SIZE
    });

    // When a complete segment is received via DC, cache + enqueue
    webrtcManager.on('segment', ({ from, segmentId, data }) => {
        const rec = {
            streamId,
            segmentId,
            data,
            sizeBytes: data.length,
            latencyMs: 0,
            speedMbps: 0,
            source: 'peer-dc',
            provider: from,
            originUrl: 'webrtc-datachannel',
            receivedAt: Date.now()
        };
        cacheSegmentLocally(ctx, rec).catch(() => undefined);
        enqueueSegment(rec);
        log.info(`Received complete segment ${segmentId} via WebRTC from ${from}`);
    });

    // When we receive a request from peer, try to send cached segment in chunks
    webrtcManager.on('request', ({ from, segmentId, channel }) => {
        const cached = getCachedSegment(ctx, segmentId);
        if (cached && channel?.readyState === 'open') {
            webrtcManager.sendSegment(channel, segmentId, cached.data);
            log.debug(`Pushed segment ${segmentId} to ${from} via WebRTC`);
        }
    });

    let shuttingDown = false;
    const gracefulShutdown = async () => {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        log.warn('Shutting down viewer...');
        await signaling.close().catch(() => undefined);
        process.exit(0);
    };

    process.on('SIGINT', gracefulShutdown);
    process.on('SIGTERM', gracefulShutdown);

    await signaling.connect();
    await ensureCacheDirectory(fallbackCfg, streamId);

    const ctx = {
        streamId,
        streamDescriptor,
        clientId,
        signaling,
        playbackCfg,
        scoringCfg,
        fallbackCfg,
        peersCfg: streamingCfg.peers,
        templates: {
            seeder: seederTemplate
        }
    };

    const buffer = new PlaybackBuffer();
    const fetchQueue = new Map();
    const retryQueue = [];
    const pendingSegments = new Map();
    const segmentSet = new Set(playlist);
    let fetchCursor = 0;
    let bufferFillCursor = 0;
    let playbackCursor = 0;

    function enqueueSegment(segment) {
        if (!segmentSet.has(segment.segmentId)) {
            buffer.push(segment);
            return;
        }
        pendingSegments.set(segment.segmentId, segment);
        while (bufferFillCursor < playlist.length) {
            const expectedId = playlist[bufferFillCursor];
            const readySegment = pendingSegments.get(expectedId);
            if (!readySegment) {
                break;
            }
            pendingSegments.delete(expectedId);
            buffer.push(readySegment);
            bufferFillCursor += 1;
        }
    }

    function requeueSegment(segmentId) {
        if (!segmentId || !segmentSet.has(segmentId)) {
            return;
        }
        if (fetchQueue.has(segmentId) || pendingSegments.has(segmentId)) {
            return;
        }
        if (!retryQueue.includes(segmentId)) {
            retryQueue.unshift(segmentId);
        }
    }

    function nextSegmentId() {
        if (retryQueue.length > 0) {
            return retryQueue.shift();
        }
        if (fetchCursor >= playlist.length) {
            return null;
        }
        return playlist[fetchCursor++];
    }

    async function scheduleSegment(segmentId, options = {}) {
        const cached = getCachedSegment(ctx, segmentId);
        if (cached) {
            enqueueSegment(cached);
            return cached;
        }
        // Try requesting via WebRTC manager first
        webrtcManager.requestSegment(segmentId);
        if (fetchQueue.has(segmentId)) {
            return fetchQueue.get(segmentId);
        }
        const task = fetchSegment(segmentId, ctx, options)
            .then((segment) => {
                enqueueSegment(segment);
                return segment;
            })
            .catch((error) => {
                log.error(`Failed to fetch segment ${segmentId}: ${error.message}`);
                requeueSegment(segmentId);
                throw error;
            })
            .finally(() => {
                fetchQueue.delete(segmentId);
            });
        fetchQueue.set(segmentId, task);
        return task;
    }

    async function ensureInitialPrefetch() {
        while (buffer.size() + pendingSegments.size < playbackCfg.minBufferPrefetch) {
            const segmentId = nextSegmentId();
            if (!segmentId) {
                break;
            }
            try {
                const shouldForceSeeder = buffer.size() === 0 && pendingSegments.size === 0 && fetchQueue.size === 0;
                const scheduleOptions = shouldForceSeeder
                    ? { forceFallback: true, reason: 'initial-prefetch' }
                    : {};
                await scheduleSegment(segmentId, scheduleOptions);
            } catch (error) {
                requeueSegment(segmentId);
                log.debug(`Retry queued for segment ${segmentId}: ${error.message}`);
                await delay(100);
            }
        }
    }

    async function ensureBufferLevels() {
        if (buffer.size() < playbackCfg.criticalBufferThreshold) {
            const segmentId = nextSegmentId();
            if (segmentId) {
                try {
                    await scheduleSegment(segmentId, { forceFallback: true, reason: 'critical-buffer' });
                } catch (error) {
                    requeueSegment(segmentId);
                    log.debug(`Force fallback retry queued for segment ${segmentId}: ${error.message}`);
                    await delay(50);
                }
            }
        }
        while (buffer.size() + pendingSegments.size + fetchQueue.size < playbackCfg.minBufferPrefetch) {
            const segmentId = nextSegmentId();
            if (!segmentId) {
                break;
            }
            const shouldForceSeeder =
                buffer.size() + pendingSegments.size + fetchQueue.size <= playbackCfg.criticalBufferThreshold;
            const scheduleOptions = shouldForceSeeder
                ? { forceFallback: true, reason: 'prefetch-low-buffer' }
                : {};
            scheduleSegment(segmentId, scheduleOptions).catch(() => {
                requeueSegment(segmentId);
            });
        }
    }

    log.info(`Viewer ${clientId} starting playback for stream ${streamId} (${playlist.length} segments)`);
    await ensureInitialPrefetch();

    while (playbackCursor < playlist.length) {
        await ensureBufferLevels();
        if (buffer.isEmpty()) {
            if (fetchQueue.size === 0 && pendingSegments.size === 0 && retryQueue.length === 0 && fetchCursor >= playlist.length) {
                break;
            }
            await delay(200);
            continue;
        }
        const segment = buffer.shift();
        playbackCursor += 1;
        log.success(`Playback segment ${segment.segmentId} via ${segment.source} (buffer=${buffer.size()})`);
        await delay(playbackCfg.playbackSegmentDurationMs);
    }

    log.success('Playback complete.');
    await gracefulShutdown();
}

// eslint-disable-next-line unicorn/prefer-top-level-await
main();

process.on('unhandledRejection', (error) => {
    const reason = error instanceof Error ? error.message : String(error);
    log.error(`Viewer terminated with error: ${reason}`);
    process.exit(1);
});
