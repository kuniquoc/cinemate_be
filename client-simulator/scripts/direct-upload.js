const fs = require('fs-extra');
const FormData = require('form-data');
const path = require('path');
const config = require('../config/config');
const { createApiClient, log, formatFileSize, formatDuration, handleApiError, validateFileType } = require('./utils');

async function testDirectUpload() {
    const startTime = Date.now();

    log.info('🎬 Starting Direct Upload Test');
    log.info('================================');

    const api = createApiClient();
    const videoFile = config.testData.sampleVideo;

    try {
        // Check if video file exists
        if (!await fs.pathExists(videoFile)) {
            log.error(`Test video file not found: ${videoFile}`);
            log.info('Please place a sample video file in the test-data/ directory');
            return;
        }

        const stats = await fs.stat(videoFile);
        const fileSize = stats.size;

        log.info(`File: ${path.basename(videoFile)}`);
        log.info(`Size: ${formatFileSize(fileSize)}`);

        // Validate file size for direct upload
        if (fileSize > 100 * 1024 * 1024) { // 100MB
            log.warn('File is quite large for direct upload. Consider using chunk upload instead.');
        }

        // Create form data - Controller expects @RequestPart("file") and @RequestPart("data")
        const form = new FormData();
        const fileStream = fs.createReadStream(videoFile);

        form.append('file', fileStream);
        // Controller expects MovieUploadRequest as @RequestPart("data") with Content-Type: application/json
        const dataBuffer = Buffer.from(JSON.stringify(config.testData.sampleMovie), 'utf8');
        form.append('data', dataBuffer, {
            filename: 'data.json',
            contentType: 'application/json'
        });

        log.info('Uploading video...');

        const uploadResponse = await api.post('/api/movies/upload', form, {
            headers: {
                ...form.getHeaders()
            },
            maxContentLength: Infinity,
            maxBodyLength: Infinity
        });

        const uploadTime = Date.now() - startTime;

        log.success(`Upload completed in ${formatDuration(uploadTime)}`);
        // MovieController now uses ResponseData wrapper
        log.info(`Movie ID: ${uploadResponse.data.data.movieId}`);
        log.info(`Status: ${uploadResponse.data.data.status}`);

        // Wait a moment and check status
        log.info('\\nChecking movie status...');

        const statusResponse = await api.get(`/api/movies/${uploadResponse.data.data.movieId}/status`);

        log.success('Status retrieved successfully');
        log.info(`Movie ID: ${statusResponse.data.data.movieId}`);
        log.info(`Status: ${statusResponse.data.data.status}`);

        if (statusResponse.data.data.qualities && Object.keys(statusResponse.data.data.qualities).length > 0) {
            log.info('Available qualities:');
            Object.entries(statusResponse.data.data.qualities).forEach(([quality, url]) => {
                log.info(`  ${quality}: ${url}`);
            });
        } else {
            log.info('No transcoded qualities available yet (processing may still be in progress)');
        }

        // Get detailed movie info
        log.info('\\nGetting movie details...');

        const infoResponse = await api.get(`/api/movies/${uploadResponse.data.data.movieId}`);

        log.success('Movie details retrieved successfully');
        log.info(`Title: ${infoResponse.data.data.title}`);
        log.info(`Description: ${infoResponse.data.data.description}`);
        log.info(`Status: ${infoResponse.data.data.status}`);

        const totalTime = Date.now() - startTime;
        log.success(`\\n✨ Direct upload test completed successfully in ${formatDuration(totalTime)}`);

        return {
            movieId: uploadResponse.data.data.movieId,
            uploadTime,
            totalTime,
            fileSize
        };

    } catch (error) {
        handleApiError(error, 'Direct upload');
        throw error;
    }
}

// Run the test if called directly
if (require.main === module) {
    testDirectUpload()
        .then((result) => {
            log.success('Test completed successfully!');
            process.exit(0);
        })
        .catch((error) => {
            log.error('Test failed!');
            process.exit(1);
        });
}

module.exports = testDirectUpload;