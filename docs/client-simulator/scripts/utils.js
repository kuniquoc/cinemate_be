const axios = require('axios');
const chalk = require('chalk');
const config = require('../config/config');

// Create axios instance with default config
const createApiClient = () => {
    return axios.create({
        baseURL: config.api.baseUrl,
        timeout: config.api.timeout,
        headers: {
            'User-Agent': 'Movie-Service-Client-Simulator/1.0.0'
        }
    });
};

// Logging utilities
const log = {
    info: (message) => {
        const timestamp = config.logging.showTimestamps ?
            chalk.gray(`[${new Date().toISOString()}] `) : '';
        console.log(timestamp + chalk.blue('ℹ') + ' ' + message);
    },

    success: (message) => {
        const timestamp = config.logging.showTimestamps ?
            chalk.gray(`[${new Date().toISOString()}] `) : '';
        console.log(timestamp + chalk.green('✓') + ' ' + message);
    },

    error: (message) => {
        const timestamp = config.logging.showTimestamps ?
            chalk.gray(`[${new Date().toISOString()}] `) : '';
        console.log(timestamp + chalk.red('✗') + ' ' + message);
    },

    warn: (message) => {
        const timestamp = config.logging.showTimestamps ?
            chalk.gray(`[${new Date().toISOString()}] `) : '';
        console.log(timestamp + chalk.yellow('⚠') + ' ' + message);
    },

    debug: (message) => {
        if (config.logging.level === 'debug') {
            const timestamp = config.logging.showTimestamps ?
                chalk.gray(`[${new Date().toISOString()}] `) : '';
            console.log(timestamp + chalk.magenta('🐛') + ' ' + message);
        }
    }
};

// Format file size
const formatFileSize = (bytes) => {
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    if (bytes === 0) return '0 Bytes';
    const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
};

// Format duration
const formatDuration = (milliseconds) => {
    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
};

// Delay function
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Retry wrapper
const withRetry = async (fn, retries = config.api.retries, delayMs = 1000) => {
    let lastError;

    for (let i = 0; i <= retries; i++) {
        try {
            return await fn();
        } catch (error) {
            lastError = error;

            if (i < retries) {
                log.warn(`Attempt ${i + 1} failed, retrying in ${delayMs}ms...`);
                await delay(delayMs);
                delayMs *= 2; // Exponential backoff
            }
        }
    }

    throw lastError;
};

// Validate file type
const validateFileType = (filename, mimeType) => {
    if (!config.upload.supportedTypes.includes(mimeType)) {
        throw new Error(`Unsupported file type: ${mimeType}. Supported types: ${config.upload.supportedTypes.join(', ')}`);
    }
};

// Handle API errors
const handleApiError = (error, operation) => {
    if (error.response) {
        // Server responded with error status
        const { status, data } = error.response;
        log.error(`${operation} failed with status ${status}`);

        if (data && data.detail) {
            log.error(`Error: ${data.detail}`);
        } else if (data && data.message) {
            log.error(`Error: ${data.message}`);
        }

        if (config.logging.level === 'debug') {
            log.debug(`Full response: ${JSON.stringify(data, null, 2)}`);
        }
    } else if (error.request) {
        // Network error
        log.error(`${operation} failed: Network error`);
        log.error(`Could not connect to ${config.api.baseUrl}`);
    } else {
        // Other error
        log.error(`${operation} failed: ${error.message}`);
    }
};

// Calculate MD5 checksum (basic implementation)
const crypto = require('crypto');
const calculateMD5 = (buffer) => {
    return crypto.createHash('md5').update(buffer).digest('hex');
};

module.exports = {
    createApiClient,
    log,
    formatFileSize,
    formatDuration,
    delay,
    withRetry,
    validateFileType,
    handleApiError,
    calculateMD5
};