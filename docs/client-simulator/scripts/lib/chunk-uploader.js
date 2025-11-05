const fs = require('fs-extra');
const FormData = require('form-data');
const path = require('path');
const config = require('../../config/config');
const {
    createApiClient,
    log,
    formatFileSize,
    withRetry,
    validateFileType,
    calculateMD5
} = require('../utils');

class ChunkUploader {
    constructor(filePath, movieData, options = {}) {
        this.filePath = filePath;
        this.movieData = movieData;
        this.api = createApiClient();
        this.uploadId = null;
        this.fileSize = 0;
        this.totalChunks = 0;
        this.uploadedChunks = new Set();
        this.progressHandler = options.onChunkUploaded;
        this.retryHandler = options.onChunkRetry;
    }

    async initiate() {
        const stats = await fs.stat(this.filePath);
        this.fileSize = stats.size;
        this.totalChunks = Math.ceil(this.fileSize / config.upload.chunkSize);

        const filename = path.basename(this.filePath);
        const mimeType = this.#detectMimeType(filename);

        validateFileType(filename, mimeType);

        const requestPayload = {
            filename,
            mimeType,
            totalSize: this.fileSize,
            chunkSize: config.upload.chunkSize,
            movieTitle: this.movieData.title,
            movieDescription: this.movieData.description
        };

        log.info(`Starting chunk upload session for ${filename}`);
        log.info(`File size: ${formatFileSize(this.fileSize)}`);
        log.info(`Chunk size: ${formatFileSize(config.upload.chunkSize)}`);
        log.info(`Total chunks: ${this.totalChunks}`);

        const response = await this.api.post('/api/movies/chunk-upload/initiate', requestPayload);
        this.uploadId = response.data.data.uploadId;

        log.success(`Upload session: ${this.uploadId}`);
        return response.data.data;
    }

    async uploadAllChunks() {
        const chunks = Array.from({ length: this.totalChunks }, (_, index) => index);
        const concurrency = config.upload.maxConcurrentChunks;

        for (let cursor = 0; cursor < chunks.length; cursor += concurrency) {
            const batch = chunks.slice(cursor, cursor + concurrency);
            const tasks = batch.map((chunkNumber) =>
                withRetry(
                    () => this.#uploadChunk(chunkNumber),
                    config.upload.chunkRetries,
                    config.upload.retryDelay
                ).catch((error) => {
                    if (this.retryHandler) {
                        this.retryHandler(chunkNumber, error);
                    }
                    throw error;
                })
            );
            await Promise.all(tasks);
        }

        log.success(`Uploaded ${this.totalChunks} chunks successfully`);
    }

    async checkStatus() {
        const response = await this.api.get(`/api/movies/chunk-upload/${this.uploadId}/status`);
        return response.data.data;
    }

    async complete() {
        const response = await this.api.post(`/api/movies/chunk-upload/${this.uploadId}/complete`);
        return response.data.data;
    }

    async cancel() {
        if (!this.uploadId) {
            return;
        }
        await this.api.delete(`/api/movies/chunk-upload/${this.uploadId}`);
    }

    async #uploadChunk(chunkNumber) {
        const start = chunkNumber * config.upload.chunkSize;
        const end = Math.min(start + config.upload.chunkSize, this.fileSize);
        const chunkSize = end - start;

        const buffer = Buffer.allocUnsafe(chunkSize);
        const fd = await fs.open(this.filePath, 'r');
        try {
            await fs.read(fd, buffer, 0, chunkSize, start);
        } finally {
            await fs.close(fd);
        }

        const checksum = calculateMD5(buffer);
        const form = new FormData();
        form.append('chunk', buffer, {
            filename: `chunk_${chunkNumber}`,
            contentType: 'application/octet-stream'
        });
        form.append('data', JSON.stringify({
            uploadId: this.uploadId,
            chunkNumber,
            chunkSize,
            checksum
        }), {
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
        if (this.progressHandler) {
            this.progressHandler(chunkNumber);
        }

        return response.data.data;
    }

    #detectMimeType(filename) {
        const extension = path.extname(filename).toLowerCase();
        const mimeMap = {
            '.mp4': 'video/mp4',
            '.avi': 'video/avi',
            '.mov': 'video/mov',
            '.mkv': 'video/mkv',
            '.webm': 'video/webm'
        };
        return mimeMap[extension] || 'video/mp4';
    }
}

module.exports = {
    ChunkUploader
};
