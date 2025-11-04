class ChunkUploader {
    constructor(apiBaseUrl, chunkSize = 5 * 1024 * 1024) { // Default 5MB
        this.apiBaseUrl = apiBaseUrl;
        this.chunkSize = chunkSize;
        this.uploadId = null;
        this.file = null;
        this.totalChunks = 0;
        this.uploadedChunks = new Set();
        this.onProgress = null;
        this.onComplete = null;
        this.onError = null;
        this.maxRetries = 3;
        this.concurrentUploads = 3;
    }

    async initiateUpload(file, movieTitle, movieDescription) {
        this.file = file;
        this.totalChunks = Math.ceil(file.size / this.chunkSize);

        const initData = {
            filename: file.name,
            mimeType: file.type,
            totalSize: file.size,
            chunkSize: this.chunkSize,
            movieTitle: movieTitle,
            movieDescription: movieDescription
        };

        try {
            const response = await fetch(`${this.apiBaseUrl}/api/movies/chunk-upload/initiate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(initData)
            });

            if (!response.ok) {
                throw new Error(`Failed to initiate upload: ${response.statusText}`);
            }

            const result = await response.json();
            this.uploadId = result.data.uploadId;

            return result.data;
        } catch (error) {
            this.handleError('Failed to initiate upload', error);
            throw error;
        }
    }

    async uploadFile() {
        if (!this.uploadId || !this.file) {
            throw new Error('Upload not initiated');
        }

        // Get current status to resume if needed
        await this.getUploadStatus();

        // Create chunk upload tasks
        const chunkTasks = [];
        for (let i = 0; i < this.totalChunks; i++) {
            if (!this.uploadedChunks.has(i)) {
                chunkTasks.push(i);
            }
        }

        // Upload chunks with concurrency control
        await this.uploadChunksConcurrently(chunkTasks);

        // Complete upload
        return await this.completeUpload();
    }

    async uploadChunksConcurrently(chunkNumbers) {
        const semaphore = new Semaphore(this.concurrentUploads);

        const uploadPromises = chunkNumbers.map(async (chunkNumber) => {
            await semaphore.acquire();
            try {
                await this.uploadChunkWithRetry(chunkNumber);
            } finally {
                semaphore.release();
            }
        });

        await Promise.all(uploadPromises);
    }

    async uploadChunkWithRetry(chunkNumber, retryCount = 0) {
        try {
            await this.uploadChunk(chunkNumber);
        } catch (error) {
            if (retryCount < this.maxRetries) {
                console.warn(`Retrying chunk ${chunkNumber}, attempt ${retryCount + 1}`);
                await new Promise(resolve => setTimeout(resolve, 1000 * (retryCount + 1)));
                return this.uploadChunkWithRetry(chunkNumber, retryCount + 1);
            }
            throw error;
        }
    }

    async uploadChunk(chunkNumber) {
        const start = chunkNumber * this.chunkSize;
        const end = Math.min(start + this.chunkSize, this.file.size);
        const chunkBlob = this.file.slice(start, end);

        // Calculate MD5 checksum
        const checksum = await this.calculateMD5(chunkBlob);

        const formData = new FormData();
        formData.append('chunk', chunkBlob);
        formData.append('data', new Blob([JSON.stringify({
            uploadId: this.uploadId,
            chunkNumber: chunkNumber,
            chunkSize: chunkBlob.size,
            checksum: checksum
        })], { type: 'application/json' }));

        const response = await fetch(
            `${this.apiBaseUrl}/api/movies/chunk-upload/${this.uploadId}/chunks/${chunkNumber}`,
            {
                method: 'POST',
                body: formData
            }
        );

        if (!response.ok) {
            throw new Error(`Failed to upload chunk ${chunkNumber}: ${response.statusText}`);
        }

        const result = await response.json();
        this.uploadedChunks.add(chunkNumber);

        // Update progress
        if (this.onProgress) {
            this.onProgress({
                uploadedChunks: this.uploadedChunks.size,
                totalChunks: this.totalChunks,
                percentage: (this.uploadedChunks.size / this.totalChunks) * 100
            });
        }

        return result;
    }

    async getUploadStatus() {
        const response = await fetch(
            `${this.apiBaseUrl}/api/movies/chunk-upload/${this.uploadId}/status`
        );

        if (!response.ok) {
            throw new Error(`Failed to get upload status: ${response.statusText}`);
        }

        const result = await response.json();
        const status = result.data;

        // Update uploaded chunks set
        status.missingChunks.forEach(chunkNum => {
            if (chunkNum >= 0 && chunkNum < this.totalChunks) {
                this.uploadedChunks.delete(chunkNum);
            }
        });

        for (let i = 0; i < this.totalChunks; i++) {
            if (!status.missingChunks.includes(i)) {
                this.uploadedChunks.add(i);
            }
        }

        return status;
    }

    async completeUpload() {
        const response = await fetch(
            `${this.apiBaseUrl}/api/movies/chunk-upload/${this.uploadId}/complete`,
            { method: 'POST' }
        );

        if (!response.ok) {
            throw new Error(`Failed to complete upload: ${response.statusText}`);
        }

        const result = await response.json();

        if (this.onComplete) {
            this.onComplete(result.data);
        }

        return result.data;
    }

    async cancelUpload() {
        if (!this.uploadId) return;

        await fetch(`${this.apiBaseUrl}/api/movies/chunk-upload/${this.uploadId}`, {
            method: 'DELETE'
        });

        this.reset();
    }

    async calculateMD5(blob) {
        return new Promise((resolve) => {
            const reader = new FileReader();
            reader.onload = function (e) {
                const arrayBuffer = e.target.result;
                const hash = CryptoJS.MD5(CryptoJS.lib.WordArray.create(arrayBuffer));
                resolve(hash.toString());
            };
            reader.readAsArrayBuffer(blob);
        });
    }

    reset() {
        this.uploadId = null;
        this.file = null;
        this.totalChunks = 0;
        this.uploadedChunks.clear();
    }

    handleError(message, error) {
        console.error(message, error);
        if (this.onError) {
            this.onError(message, error);
        }
    }
}

// Semaphore for controlling concurrency
class Semaphore {
    constructor(permits) {
        this.permits = permits;
        this.waiting = [];
    }

    async acquire() {
        if (this.permits > 0) {
            this.permits--;
            return Promise.resolve();
        }

        return new Promise(resolve => {
            this.waiting.push(resolve);
        });
    }

    release() {
        this.permits++;
        if (this.waiting.length > 0) {
            const resolve = this.waiting.shift();
            this.permits--;
            resolve();
        }
    }
}

// Usage example (browser environment):
// const uploader = new ChunkUploader('http://localhost:8080');
// uploader.onProgress = (progress) => console.log(`Progress: ${progress.percentage.toFixed(2)}%`);
// uploader.onComplete = (result) => console.log('Upload completed:', result);
// uploader.onError = (message, error) => console.error('Upload error:', message, error);
// const file = document.getElementById('video-file').files[0];
// await uploader.initiateUpload(file, 'My Movie', 'Movie description');
// await uploader.uploadFile();

if (typeof module !== 'undefined') {
    module.exports = { ChunkUploader, Semaphore };
}
