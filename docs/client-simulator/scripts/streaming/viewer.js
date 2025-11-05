#!/usr/bin/env node

const path = require('path');
const fs = require('fs-extra');
const axios = require('axios');
const crypto = require('crypto');
const chalk = require('chalk');

const config = require('../../config/config');
const { log, delay, createApiClient, handleApiError } = require('../utils');
const SignalingClient = require('./signaling-client');
const PlaybackBuffer = require('./playback-buffer');
const { scorePeers } = require('./peer-selector');

const segmentCache = new Map();
const DEFAULT_SEEDER_TEMPLATE = '/streams/{streamId}/segments/{segmentId}';
const DEFAULT_ORIGIN_TEMPLATE = '/{movieId}/{quality}/{segmentId}.ts';

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
        try {
            const manifestSegments = await readManifest(playlistCfg.manifestPath);
            if (manifestSegments.length > 0) {
                return manifestSegments;
            }
        } catch (error) {
            log.warn(`Failed to read manifest at ${playlistCfg.manifestPath}: ${error.message}`);
        }
    }
    const manifestSegments = await loadSegmentsFromMovieInfo(streamDescriptor, streamingCfg);
    if (manifestSegments.length > 0) {
        return manifestSegments;
    }
    const total = Number(args.segments ?? playlistCfg.defaultSegmentCount);
    const start = Number(args.start ?? playlistCfg.startIndex);
    const segments = [];
    for (let idx = 0; idx < total; idx++) {
        const value = formatSegmentId(start + idx, playlistCfg);
        segments.push(value);
    }
    return segments;
}

async function readManifest(filePath) {
    const content = await fs.readFile(filePath, 'utf8');
    return content
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0 && !line.startsWith('#'));
}

function formatSegmentId(index, playlistCfg) {
    const padded = String(index).padStart(playlistCfg.indexPadLength ?? 4, '0');
    return playlistCfg.segmentIdTemplate.replace('{index}', padded);
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
            log.info(`Đã tải manifest chất lượng ${qualityKey} với ${segments.length} segment từ ${manifestUrl}`);
        }
        return segments;
    } catch (error) {
        if (error.response) {
            log.warn(`Không thể tải manifest ${manifestUrl ?? ''} (HTTP ${error.response.status})`);
        } else if (error.request) {
            log.warn(`Không thể kết nối tới ${manifestUrl ?? 'manifest'}: ${error.message}`);
        } else {
            log.warn(`Không thể tải manifest: ${error.message}`);
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
            log.warn(`Movie ${streamDescriptor.movieId} không có thông tin quality trong API.`);
            return { qualityKey: null, manifestUrl: null };
        }
        const qualityEntry = pickQualityEntry(qualities, streamDescriptor.quality);
        if (!qualityEntry) {
            log.warn(`Không tìm thấy quality "${streamDescriptor.quality}" trong phản hồi API.`);
            return { qualityKey: null, manifestUrl: null };
        }
        const manifestUrl = resolveManifestUrl(qualityEntry.url, streamingCfg);
        if (!manifestUrl) {
            log.warn(`Không xây dựng được URL manifest cho quality ${qualityEntry.key}.`);
            return { qualityKey: null, manifestUrl: null };
        }
        return { qualityKey: qualityEntry.key, manifestUrl };
    } catch (error) {
        handleApiError(error, 'Gọi API movie info');
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
    const baseClean = originBase.replace(/\/+$/g, '');
    const pathClean = rawUrl.replace(/^\/+/, '');
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

async function httpFetchFromSeederOrOrigin(ctx, segmentId) {
    const endpoints = [];
    if (ctx.fallbackCfg.seeder?.baseUrl) {
        endpoints.push({
            name: 'seeder',
            baseUrl: ctx.fallbackCfg.seeder.baseUrl,
            template: ctx.templates.seeder
        });
    }
    if (ctx.fallbackCfg.origin?.baseUrl) {
        if (ctx.streamDescriptor.movieId && ctx.streamDescriptor.quality) {
            endpoints.push({
                name: 'origin',
                baseUrl: ctx.fallbackCfg.origin.baseUrl,
                template: ctx.templates.origin
            });
        } else if (!ctx.originDescriptorWarningShown) {
            log.warn('Bỏ qua fallback Origin vì streamId "%s" không chứa movieId_quality.', ctx.streamDescriptor.streamId);
            ctx.originDescriptorWarningShown = true;
        }
    }
    let lastError = null;
    for (const endpoint of endpoints) {
        const url = buildSegmentUrl(endpoint.baseUrl, endpoint.template, ctx.streamDescriptor, segmentId);
        if (!url) {
            continue;
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
                reportSource: endpoint.name === 'seeder' ? 'seeder' : 'origin',
                url
            };
        } catch (error) {
            lastError = error;
            const message = error.response ? `HTTP ${error.response.status}` : error.message;
            log.warn(`Fallback ${endpoint.name} failed for segment ${segmentId}: ${message}`);
        }
    }
    if (lastError) {
        throw lastError;
    }
    throw new Error('No fallback endpoint configured');
}

async function fetchSegment(segmentId, ctx, options = {}) {
    const cached = getCachedSegment(ctx, segmentId);
    if (cached) {
        log.success(`${chalk.green('Cache hit')} segment ${segmentId}`);
        return cached;
    }

    log.info(`Viewer requesting segment ${segmentId} of stream ${ctx.streamId}`);
    let peers = [];
    if (!options.forceFallback) {
        try {
            peers = await ctx.signaling.requestWhoHas(segmentId, ctx.playbackCfg.whoHasQueryTimeoutMs);
        } catch (error) {
            log.warn(`WHO_HAS failed for segment ${segmentId}: ${error.message}`);
        }
    }

    let result = null;
    if (!options.forceFallback && Array.isArray(peers) && peers.length > 0) {
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
        log.warn(`All peer attempts failed for segment ${segmentId} — fallback to Seeder/Origin`);
        result = await httpFetchFromSeederOrOrigin(ctx, segmentId);
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

(async () => {
    const args = parseArgs(process.argv);
    if (!config.streaming) {
        log.error('Streaming simulator configuration not found in config/config.js');
        process.exit(1);
    }

    const streamingCfg = config.streaming;
    const streamId = args.stream || streamingCfg.defaultStreamId;
    if (!streamId) {
        log.error('Thiếu streamId. Truyền bằng --stream=<movieId>_<quality> hoặc cấu hình STREAMING_DEFAULT_STREAM_ID.');
        process.exit(1);
    }
    const streamDescriptor = parseStreamDescriptor(streamId);
    if (!streamDescriptor.movieId || !streamDescriptor.quality) {
        log.warn('streamId "%s" không theo định dạng <movieId>_<quality>. Seeder vẫn hoạt động, nhưng fallback Origin sẽ bị tắt.', streamDescriptor.streamId);
    }
    const clientId = args.client || `${streamingCfg.defaultClientPrefix}${crypto.randomUUID()}`;
    const playlist = await loadPlaylist(args, streamingCfg.playlist, streamDescriptor, streamingCfg);

    if (!playlist.length) {
        log.error('Playlist is empty. Provide --segments or a manifest file.');
        process.exit(1);
    }

    const playbackCfg = streamingCfg.playback;
    const scoringCfg = streamingCfg.scoring;
    const fallbackCfgSource = streamingCfg.fallback || {};
    const fallbackCfg = {
        persistCacheToDisk: fallbackCfgSource.persistCacheToDisk !== undefined ? fallbackCfgSource.persistCacheToDisk : true,
        cacheDirectory: fallbackCfgSource.cacheDirectory || './.cache/streaming',
        seeder: fallbackCfgSource.seeder || {},
        origin: fallbackCfgSource.origin || {}
    };
    const seederTemplate = fallbackCfg.seeder.segmentPathTemplate || DEFAULT_SEEDER_TEMPLATE;
    const originTemplate = fallbackCfg.origin.segmentPathTemplate || DEFAULT_ORIGIN_TEMPLATE;

    const signaling = new SignalingClient({
        url: streamingCfg.signaling.wsBaseUrl,
        clientId,
        streamId,
        logger: log
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
            seeder: seederTemplate,
            origin: originTemplate
        },
        originDescriptorWarningShown: false
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
                await scheduleSegment(segmentId);
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
                    await scheduleSegment(segmentId, { forceFallback: true });
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
            scheduleSegment(segmentId).catch(() => {
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
        log.success(`${chalk.cyan('Playback')} segment ${segment.segmentId} via ${segment.source} (buffer=${buffer.size()})`);
        await delay(playbackCfg.playbackSegmentDurationMs);
    }

    log.success('Playback complete.');
    await gracefulShutdown();
})();
