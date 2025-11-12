const path = require('node:path');

const toPositiveNumber = (value, fallback) => {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
        return fallback;
    }
    return parsed;
};

const baseUrl = process.env.MOVIE_SERVICE_BASE_URL || 'http://localhost:8080';
const apiTimeout = toPositiveNumber(process.env.MOVIE_SERVICE_TIMEOUT_MS, 30000);
const chunkSizeMb = toPositiveNumber(process.env.MOVIE_UPLOAD_CHUNK_MB, 5);
const maxConcurrentChunks = Math.max(1, Math.floor(toPositiveNumber(process.env.MOVIE_UPLOAD_CONCURRENCY, 3)));

const seederBaseUrlEnv = process.env.STREAMING_SEEDER_BASE_URL;
const originBaseUrlEnv = process.env.STREAMING_ORIGIN_BASE_URL;
const seederBaseUrlValue = seederBaseUrlEnv === undefined ? 'http://localhost:8084' : seederBaseUrlEnv.trim();
const originBaseUrlValue = originBaseUrlEnv === undefined ? 'http://localhost:9000/movies' : originBaseUrlEnv.trim();
const seederBaseUrl = seederBaseUrlValue.length ? seederBaseUrlValue : null;
const originBaseUrl = originBaseUrlValue.length ? originBaseUrlValue : null;
const seederSegmentTemplate = (process.env.STREAMING_SEEDER_SEGMENT_TEMPLATE
    || '/streams/{streamId}/segments/{segmentId}').trim();
const defaultStreamId = (process.env.STREAMING_DEFAULT_STREAM_ID || '').trim() || null;

const resolveTestData = (fileName) => path.join(__dirname, '..', 'test-data', fileName);

module.exports = {
    // Movie Service API Configuration
    api: {
        baseUrl,
        timeout: apiTimeout,
        retries: 3
    },

    // Upload Configuration
    upload: {
        // Chunk size for chunk upload (5MB default)
        chunkSize: chunkSizeMb * 1024 * 1024,

        // Maximum concurrent chunk uploads
        maxConcurrentChunks,

        // Retry configuration for failed chunks
        chunkRetries: 3,
        retryDelay: 1000, // 1 second

        // Supported file types
        supportedTypes: [
            'video/mp4',
            'video/avi',
            'video/mov',
            'video/mkv',
            'video/webm'
        ]
    },

    // Test Data Configuration
    testData: {
        sampleVideo: resolveTestData('sample-video.mp4'),
        largeVideo: resolveTestData('large-video.mp4'),

        // Test movie metadata
        sampleMovie: {
            title: 'Sample Test Movie',
            description: 'This is a test movie uploaded via direct upload'
        },

        largeMovie: {
            title: 'Large Test Movie',
            description: 'This is a large test movie uploaded via chunk upload'
        }
    },

    // Logging Configuration
    logging: {
        level: 'info', // debug, info, warn, error
        showProgress: true,
        showTimestamps: true
    },

    // Streaming playback simulator configuration
    streaming: {
        defaultStreamId,
        defaultClientPrefix: 'viewer-',
        playlist: {
            // Number of segments to simulate when no manifest is provided
            defaultSegmentCount: 12,
            // Index to start generating segment identifiers from
            startIndex: 0,
            // Template for generated segment identifiers. {index} will be replaced by a zero-padded number.
            segmentIdTemplate: 'seg_{index}',
            // Number of digits to pad the index with when generating identifiers
            indexPadLength: 3,
            // Optional path to a manifest (newline separated list of segment ids). Leave null to auto-generate.
            manifestPath: null
        },
        playback: {
            maxActivePeers: 3,
            peerConnectTimeoutMs: 5000,
            segmentRequestWaitMinMs: 120,
            segmentRequestWaitMaxMs: 200,
            whoHasQueryTimeoutMs: 150,
            fallbackHttpTimeoutMs: 700,
            minBufferPrefetch: 3,
            criticalBufferThreshold: 1,
            playbackSegmentDurationMs: 4000
        },
        scoring: {
            alphaSpeed: 0.6,
            betaLatency: 0.002,
            gammaReliability: 0.4
        },
        signaling: {
            wsBaseUrl: 'ws://localhost:8083/ws/signaling'
        },
        webrtc: {
            enabled: true,
            stunServers: [
                'stun:stun.l.google.com:19302',
                'stun:global.stun.twilio.com:3478'
            ]
        },
        fallback: {
            seeder: {
                baseUrl: seederBaseUrl,
                segmentPathTemplate: seederSegmentTemplate
            },
            origin: {
                baseUrl: originBaseUrl
            },
            persistCacheToDisk: true,
            cacheDirectory: './.cache/streaming'
        },
        peers: {
            // Configure known peer endpoints here when running multiple simulators
            // Example: "edge-a": { baseUrl: "http://localhost:9101" }
        }
    }
};