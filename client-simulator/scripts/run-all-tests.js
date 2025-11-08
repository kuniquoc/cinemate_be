const testDirectUpload = require('./direct-upload');
const testChunkUpload = require('./chunk-upload');
const { createApiClient, log, formatFileSize, formatDuration } = require('./utils');

async function testApiConnection() {
    const api = createApiClient();
    try {
        await api.get('/api/movies/chunk-upload/client.js');
        log.success('API reachable');
        return true;
    } catch (error) {
        log.error('Cannot reach Movie Service API');
        log.error(error.message);
        return false;
    }
}

async function runAllTests() {
    const startTime = Date.now();

    log.info('🎬 Running Complete Movie Service Test Suite');
    log.info('=============================================');

    try {
        // Step 1: Test API connection
        log.info('\n📡 Step 1: Testing API Connection');
        const connected = await testApiConnection();

        if (!connected) {
            log.error('Cannot proceed - API connection failed');
            process.exit(1);
        }

        // Step 2: Test Direct Upload
        log.info('\n📤 Step 2: Testing Direct Upload');
        let directUploadResult;
        try {
            directUploadResult = await testDirectUpload();
            log.success('Direct upload test passed');
        } catch (error) {
            log.error('Direct upload test failed:', error.message);
            // Continue with other tests
        }

        // Step 3: Test Chunk Upload
        log.info('\n🧩 Step 3: Testing Chunk Upload');
        let chunkUploadResult;
        try {
            chunkUploadResult = await testChunkUpload();
            log.success('Chunk upload test passed');
        } catch (error) {
            log.error('Chunk upload test failed:', error.message);
            // Continue
        }

        // Summary
        const totalTime = Date.now() - startTime;
        log.info('\n📊 Test Summary');
        log.info('================');

        if (directUploadResult) {
            log.success(`✅ Direct Upload: Movie ID ${directUploadResult.movieId}`);
            log.info(`   File size: ${formatFileSize(directUploadResult.fileSize)}`);
            log.info(`   Upload time: ${formatDuration(directUploadResult.uploadTime)}`);
        } else {
            log.error('❌ Direct Upload: Failed');
        }

        if (chunkUploadResult) {
            log.success(`✅ Chunk Upload: Movie ID ${chunkUploadResult.movieId}`);
            log.info(`   File size: ${formatFileSize(chunkUploadResult.fileSize)}`);
            log.info(`   Total chunks: ${chunkUploadResult.totalChunks}`);
            log.info(`   Upload time: ${formatDuration(chunkUploadResult.uploadTime)}`);
        } else {
            log.error('❌ Chunk Upload: Failed');
        }

        log.info(`\nTotal test time: ${formatDuration(totalTime)}`);

        if (directUploadResult || chunkUploadResult) {
            log.success('\n✨ Test suite completed with some successes!');

            // Provide next steps
            log.info('\n🎯 Next Steps:');
            if (directUploadResult) {
                log.info(`• Check direct upload movie status: node scripts/movie-status.js ${directUploadResult.movieId}`);
            }
            if (chunkUploadResult) {
                log.info(`• Check chunk upload movie status: node scripts/movie-status.js ${chunkUploadResult.movieId}`);
            }
            log.info('• Wait a few minutes for transcoding to complete');
            log.info('• Check movie status again to see transcoded qualities');

        } else {
            log.error('\n❌ All tests failed');
            process.exit(1);
        }

    } catch (error) {
        log.error('Test suite failed:', error.message);
        process.exit(1);
    }
}

// Run if called directly
if (require.main === module) {
    runAllTests();
}

module.exports = runAllTests;