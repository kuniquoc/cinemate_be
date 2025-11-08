const fs = require('fs-extra');
const ProgressBar = require('progress');
const config = require('../config/config');
const { createApiClient, log, formatDuration, handleApiError } = require('./utils');
const { ChunkUploader } = require('./lib/chunk-uploader');

async function testChunkUpload() {
    const startedAt = Date.now();

    log.info('🎬 Starting Chunk Upload Test');
    log.info('==============================');

    const primaryFile = config.testData.largeVideo;

    try {
        if (await fs.pathExists(primaryFile)) {
            return await runUpload(primaryFile, config.testData.largeMovie, startedAt);
        }

        log.warn(`Test video not found: ${primaryFile}`);
        const fallbackFile = config.testData.sampleVideo;

        if (await fs.pathExists(fallbackFile)) {
            log.info(`Using fallback file: ${fallbackFile}`);
            return await runUpload(fallbackFile, config.testData.sampleMovie, startedAt);
        }

        log.error('No test video available. Please add files to test-data/.');
        return null;
    } catch (error) {
        handleApiError(error, 'Chunk upload');
        throw error;
    }
}

async function runUpload(filePath, movieMetadata, startedAt) {
    let progress = null;
    let progressTicks = 0;
    const uploader = new ChunkUploader(filePath, movieMetadata, {
        onChunkUploaded: () => {
            if (!progress) {
                return;
            }
            progress.tick();
            progressTicks += 1;
        },
        onChunkRetry: (chunkNumber, error) => {
            log.error(`Chunk ${chunkNumber} failed: ${error.message}`);
        }
    });

    try {
        await uploader.initiate();

        if (config.logging.showProgress) {
            progress = new ProgressBar('Uploading [:bar] :percent :current/:total chunks (:etas remaining)', {
                complete: '#',
                incomplete: ' ',
                width: 36,
                total: uploader.totalChunks
            });
        }

        const uploadStartedAt = Date.now();
        await uploader.uploadAllChunks();
        const uploadDuration = Date.now() - uploadStartedAt;

        const status = await uploader.checkStatus();
        console.log('');
        log.info(`Upload progress reported: ${status.progressPercentage}%`);
        log.info(`Uploaded ${status.uploadedChunks}/${status.totalChunks} chunks`);

        if (Array.isArray(status.missingChunks) && status.missingChunks.length > 0) {
            throw new Error(`Missing chunks detected: ${status.missingChunks.join(', ')}`);
        }

        const movieData = await uploader.complete();
        log.success('Upload completed by server merge');
        log.info(`Movie ID: ${movieData.movieId}`);
        log.info(`Status: ${movieData.status}`);

        const totalDuration = Date.now() - startedAt;
        console.log('');
        log.success('✨ Chunk upload test finished');
        log.info(`Upload duration: ${formatDuration(uploadDuration)}`);
        log.info(`Total duration: ${formatDuration(totalDuration)}`);
        log.info(`Chunks uploaded: ${progressTicks || uploader.totalChunks}`);

        const api = createApiClient();
        const movieResponse = await api.get(`/api/movies/${movieData.movieId}`);

        log.success('Movie data verified');
        log.info(`Title: ${movieResponse.data.data.title}`);
        log.info(`Description: ${movieResponse.data.data.description}`);
        log.info(`Current status: ${movieResponse.data.data.status}`);

        return {
            movieId: movieData.movieId,
            uploadId: uploader.uploadId,
            uploadTime: uploadDuration,
            totalTime: totalDuration,
            fileSize: uploader.fileSize,
            totalChunks: uploader.totalChunks
        };
    } catch (error) {
        try {
            await uploader.cancel();
        } catch (cancelError) {
            log.warn(`Failed to cancel upload session: ${cancelError.message}`);
        }
        throw error;
    }
}

process.on('SIGINT', () => {
    log.warn('\nReceived SIGINT');
    process.exit(1);
});

if (require.main === module) {
    testChunkUpload()
        .then(() => {
            log.success('Test completed successfully!');
            process.exit(0);
        })
        .catch(() => {
            log.error('Test failed!');
            process.exit(1);
        });
}

module.exports = testChunkUpload;