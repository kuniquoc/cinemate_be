module.exports = {
    // Movie Service API Configuration
    api: {
        baseUrl: 'http://localhost:8080', // movie-service runs on port 8080 according to docker-compose
        timeout: 30000, // 30 seconds
        retries: 3
    },

    // Upload Configuration
    upload: {
        // Chunk size for chunk upload (5MB default)
        chunkSize: 5 * 1024 * 1024,

        // Maximum concurrent chunk uploads
        maxConcurrentChunks: 3,

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
        sampleVideo: './test-data/sample-video.mp4',
        largeVideo: './test-data/large-video.mp4',

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
    }
};