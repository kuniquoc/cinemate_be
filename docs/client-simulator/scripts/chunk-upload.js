const fs = require('fs-extra');
const FormData = require('form-data');
const path = require('path');
const ProgressBar = require('progress');
const config = require('../config/config');
const {
    createApiClient,
    log,
    formatFileSize,
    formatDuration,
    delay,
    withRetry,
    handleApiError,
    validateFileType,
    calculateMD5
} = require('./utils');

class ChunkUploader {
    constructor(filePath, movieData) {
        this.filePath = filePath;
        this.movieData = movieData;
        this.api = createApiClient();
        this.uploadId = null;
        this.totalChunks = 0;
        this.uploadedChunks = new Set();
        this.fileSize = 0;
        this.progressBar = null;
    }

    async initiate() {
        const stats = await fs.stat(this.filePath);
        this.fileSize = stats.size;
        this.totalChunks = Math.ceil(this.fileSize / config.upload.chunkSize);

        const filename = path.basename(this.filePath);
        const mimeType = this.getMimeType(filename);

        validateFileType(filename, mimeType);

        const initRequest = {
            filename,
            mimeType,
            totalSize: this.fileSize,
            chunkSize: config.upload.chunkSize,
            movieTitle: this.movieData.title,
            movieDescription: this.movieData.description
        };

        log.info(`Initiating chunk upload for ${filename}`);
        log.info(`File size: ${formatFileSize(this.fileSize)}`);
        log.info(`Total chunks: ${this.totalChunks}`);
        log.info(`Chunk size: ${formatFileSize(config.upload.chunkSize)}`);

        const response = await this.api.post('/api/movies/chunk-upload/initiate', initRequest);

        // ChunkUploadController returns ResponseData wrapper
        this.uploadId = response.data.data.uploadId;

        log.success(`Upload session initiated: ${this.uploadId}`);
        log.info(`Session expires at: ${new Date(response.data.data.expiresAt).toLocaleString()}`);

        return response.data.data;
    }

    async uploadChunk(chunkNumber) {
        const start = chunkNumber * config.upload.chunkSize;
        const end = Math.min(start + config.upload.chunkSize, this.fileSize);
        const chunkSize = end - start;

        // Read chunk data
        const buffer = Buffer.allocUnsafe(chunkSize);
        const fd = await fs.open(this.filePath, 'r');
        await fs.read(fd, buffer, 0, chunkSize, start);
        await fs.close(fd);

        // Calculate checksum
        const checksum = calculateMD5(buffer);

        // Create form data
        const form = new FormData();
        form.append('chunk', buffer, {
            filename: `chunk_${chunkNumber}`,
            contentType: 'application/octet-stream'
        });

        const chunkData = {
            uploadId: this.uploadId,
            chunkNumber,
            chunkSize,
            checksum
        };

        form.append('data', JSON.stringify(chunkData), {
            contentType: 'application/json'
        });

        const response = await this.api.post(
            `/api/movies/chunk-upload/${this.uploadId}/chunks/${chunkNumber}`,
            form,
            {
                headers: {
                    ...form.getHeaders()
                },
                maxContentLength: Infinity,
                maxBodyLength: Infinity
            }
        );

        this.uploadedChunks.add(chunkNumber);

        if (this.progressBar) {
            this.progressBar.tick();
        }

        return response.data.data;
    }

    async uploadAllChunks() {
        log.info('Starting chunk upload...');

        if (config.logging.showProgress) {
            this.progressBar = new ProgressBar(
                'Uploading [:bar] :percent :current/:total chunks (:etas remaining)',
                {
                    complete: '█',
                    incomplete: '░',
                    width: 40,
                    total: this.totalChunks
                }
            );
        }

        const concurrency = config.upload.maxConcurrentChunks;
        const chunks = Array.from({ length: this.totalChunks }, (_, i) => i);

        // Upload chunks with controlled concurrency
        for (let i = 0; i < chunks.length; i += concurrency) {
            const batch = chunks.slice(i, i + concurrency);

            const promises = batch.map(chunkNumber =>
                withRetry(
                    () => this.uploadChunk(chunkNumber),
                    config.upload.chunkRetries,
                    config.upload.retryDelay
                ).catch(error => {
                    log.error(`Failed to upload chunk ${chunkNumber}: ${error.message}`);
                    throw error;
                })
            );

            await Promise.all(promises);
        }

        log.success(`\\nAll ${this.totalChunks} chunks uploaded successfully`);
    }

    async checkStatus() {
        const response = await this.api.get(`/api/movies/chunk-upload/${this.uploadId}/status`);
        return response.data.data;
    }

    async complete() {
        log.info('Completing upload and merging chunks...');

        const response = await this.api.post(`/api/movies/chunk-upload/${this.uploadId}/complete`);

        log.success('Upload completed successfully');
        log.info(`Movie ID: ${response.data.data.movieId}`);
        log.info(`Status: ${response.data.data.status}`);

        return response.data.data;
    }

    async cancel() {
        if (this.uploadId) {
            log.info('Cancelling upload...');
            await this.api.delete(`/api/movies/chunk-upload/${this.uploadId}`);
            log.success('Upload cancelled successfully');
        }
    }

    getMimeType(filename) {
        const ext = path.extname(filename).toLowerCase();
        const mimeTypes = {
            '.mp4': 'video/mp4',
            '.avi': 'video/avi',
            '.mov': 'video/mov',
            '.mkv': 'video/mkv',
            '.webm': 'video/webm'
        };
        return mimeTypes[ext] || 'video/mp4';
    }
}

async function testChunkUpload() {
    const startTime = Date.now();

    log.info('🎬 Starting Chunk Upload Test');
    log.info('==============================');

    const videoFile = config.testData.largeVideo;

    try {
        // Check if video file exists
        if (!await fs.pathExists(videoFile)) {
            log.error(`Test video file not found: ${videoFile}`);
            log.info('Please place a large video file in the test-data/ directory');

            // Try fallback to sample video
            const fallbackFile = config.testData.sampleVideo;
            if (await fs.pathExists(fallbackFile)) {
                log.info(`Using fallback file: ${fallbackFile}`);
                return await testChunkUploadWithFile(fallbackFile, startTime);
            }

            return;
        }

        return await testChunkUploadWithFile(videoFile, startTime);

    } catch (error) {
        handleApiError(error, 'Chunk upload');
        throw error;
    }
}

async function testChunkUploadWithFile(videoFile, startTime) {
    const uploader = new ChunkUploader(videoFile, config.testData.largeMovie);

    try {
        // Step 1: Initiate upload
        await uploader.initiate();

        // Step 2: Upload all chunks
        const uploadStartTime = Date.now();
        await uploader.uploadAllChunks();
        const uploadTime = Date.now() - uploadStartTime;

        // Step 3: Check status before completing
        log.info('\\nChecking upload status...');
        const status = await uploader.checkStatus();

        log.info(`Upload progress: ${status.progressPercentage}%`);
        log.info(`Uploaded chunks: ${status.uploadedChunks}/${status.totalChunks}`);

        if (status.missingChunks && status.missingChunks.length > 0) {
            log.warn(`Missing chunks: ${status.missingChunks.join(', ')}`);
            throw new Error('Upload incomplete - missing chunks detected');
        }

        // Step 4: Complete upload
        const movieData = await uploader.complete();

        const totalTime = Date.now() - startTime;

        log.success(`\\n✨ Chunk upload test completed successfully!`);
        log.info(`Upload time: ${formatDuration(uploadTime)}`);
        log.info(`Total time: ${formatDuration(totalTime)}`);
        log.info(`Movie ID: ${movieData.movieId}`);

        // Step 5: Verify movie was created
        log.info('\\nVerifying movie creation...');
        const api = createApiClient();
        const movieResponse = await api.get(`/api/movies/${movieData.movieId}`);

        log.success('Movie verified successfully');
        // MovieController now uses ResponseData wrapper
        log.info(`Title: ${movieResponse.data.data.title}`);
        log.info(`Description: ${movieResponse.data.data.description}`);
        log.info(`Status: ${movieResponse.data.data.status}`);

        return {
            movieId: movieData.movieId,
            uploadId: uploader.uploadId,
            uploadTime,
            totalTime,
            fileSize: uploader.fileSize,
            totalChunks: uploader.totalChunks
        };

    } catch (error) {
        // Try to cancel upload on error
        try {
            await uploader.cancel();
        } catch (cancelError) {
            log.warn('Failed to cancel upload:', cancelError.message);
        }
        throw error;
    }
}

// Handle process termination gracefully
process.on('SIGINT', async () => {
    log.warn('\\nReceived SIGINT, attempting to cancel upload...');
    // In a real implementation, you'd store the uploader instance globally
    // and cancel it here
    process.exit(1);
});

// Run the test if called directly
if (require.main === module) {
    testChunkUpload()
        .then((result) => {
            log.success('Test completed successfully!');
            process.exit(0);
        })
        .catch((error) => {
            log.error('Test failed!');
            process.exit(1);
        });
}

module.exports = testChunkUpload;