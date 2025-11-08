#!/usr/bin/env node

const fs = require('fs-extra');
const config = require('./config/config');
const { log, formatFileSize, formatDuration } = require('./scripts/utils');
const testDirectUpload = require('./scripts/direct-upload');
const testChunkUpload = require('./scripts/chunk-upload');
const { testMovieStatus, monitorMovieStatus } = require('./scripts/movie-status');

// Command line argument parsing
const args = process.argv.slice(2);
const command = args[0];

function showHelp() {
    console.log(`
🎬 Movie Service Client Simulator

Usage: node index.js <command> [options]

Commands:
  test:direct              Test direct movie upload
  test:chunk              Test chunked movie upload
  test:status <movieId>   Test movie status endpoints
  test:monitor <movieId>  Monitor movie processing status
  test:all                Run all tests (direct + chunk)
  help                    Show this help message

Examples:
  node index.js test:direct
  node index.js test:chunk
  node index.js test:status 123e4567-e89b-12d3-a456-426614174000
  node index.js test:monitor 123e4567-e89b-12d3-a456-426614174000
  node index.js test:all

Options:
  --config <file>         Use custom config file
  --verbose               Enable verbose logging

Setup:
  1. Make sure Movie Service is running on ${config.api.baseUrl}
  2. Place test video files in test-data/ directory
  3. Run 'npm install' to install dependencies

Configuration:
  Edit config/config.js to customize settings
  `);
}

async function checkSetup() {
    log.info('🔍 Checking setup...');

    // Check if config exists
    if (!await fs.pathExists('./config/config.js')) {
        log.error('Configuration file not found: config/config.js');
        return false;
    }

    // Check if test data directory exists
    if (!await fs.pathExists('./test-data')) {
        log.warn('Test data directory not found, creating it...');
        await fs.ensureDir('./test-data');
    }

    // Check for test video files
    const sampleVideoExists = await fs.pathExists(config.testData.sampleVideo);
    const largeVideoExists = await fs.pathExists(config.testData.largeVideo);

    if (!sampleVideoExists && !largeVideoExists) {
        log.error('No test video files found in test-data/ directory');
        log.info('Please add video files as described in test-data/README.md');
        return false;
    }

    if (!sampleVideoExists) {
        log.warn('Sample video file not found, some tests may be skipped');
    }

    if (!largeVideoExists) {
        log.warn('Large video file not found, chunk upload test may use sample video');
    }

    log.success('Setup check completed');
    return true;
}

async function runTests() {
    const setupOk = await checkSetup();
    if (!setupOk) {
        process.exit(1);
    }

    log.info('\n🚀 Starting Movie Service API Tests');
    log.info('====================================');

    const results = {
        direct: null,
        chunk: null,
        startTime: Date.now()
    };

    try {
        // Test direct upload
        if (await fs.pathExists(config.testData.sampleVideo)) {
            log.info(`\n🎯 Running Direct Upload Test...`);
            results.direct = await testDirectUpload();
            log.success(`Direct upload test completed successfully\n`);
        } else {
            log.warn('Skipping direct upload test - no sample video file\n');
        }

        // Test chunk upload
        log.info('🎯 Running Chunk Upload Test...');
        results.chunk = await testChunkUpload();
        log.success('Chunk upload test completed successfully\n');

        // Summary
        const totalTime = Date.now() - results.startTime;

        log.info(`\n📊 Test Summary`);
        log.info('================');

        if (results.direct) {
            log.info(`✅ Direct Upload: ${results.direct.movieId}`);
            log.info(`   File size: ${formatFileSize(results.direct.fileSize)}`);
            log.info(`   Upload time: ${formatDuration(results.direct.uploadTime)}`);
        }

        if (results.chunk) {
            log.info(`✅ Chunk Upload: ${results.chunk.movieId}`);
            log.info(`   File size: ${formatFileSize(results.chunk.fileSize)}`);
            log.info(`   Total chunks: ${results.chunk.totalChunks}`);
            log.info(`   Upload time: ${formatDuration(results.chunk.uploadTime)}`);
        }

        log.info(`\n⏱️  Total test time: ${formatDuration(totalTime)}`);
        log.success(`\n🎉 All tests completed successfully!`);

        return results;

    } catch (error) {
        log.error(`\n❌ Tests failed!`);
        throw error;
    }
}

async function main() {
    try {
        // Handle verbose flag
        if (args.includes('--verbose')) {
            config.logging.level = 'debug';
        }

        // Handle custom config
        const configIndex = args.indexOf('--config');
        if (configIndex !== -1 && args[configIndex + 1]) {
            // In a real implementation, you'd load the custom config here
            log.info(`Using custom config: ${args[configIndex + 1]}`);
        }

        switch (command) {
            case 'test:direct':
                await checkSetup();
                await testDirectUpload();
                break;

            case 'test:chunk':
                await checkSetup();
                await testChunkUpload();
                break;

            case 'test:status': {
                const movieId = args[1];
                if (!movieId) {
                    log.error('Movie ID is required for status test');
                    log.info('Usage: node index.js test:status <movieId>');
                    process.exit(1);
                }
                await testMovieStatus(movieId);
                break;
            }

            case 'test:monitor': {
                const monitorMovieId = args[1];
                if (!monitorMovieId) {
                    log.error('Movie ID is required for monitoring');
                    log.info('Usage: node index.js test:monitor <movieId>');
                    process.exit(1);
                }
                await monitorMovieStatus(monitorMovieId);
                break;
            }

            case 'test:all':
                await runTests();
                break;

            case 'help':
            case '--help':
            case '-h':
                showHelp();
                break;

            default:
                if (!command) {
                    showHelp();
                } else {
                    log.error(`Unknown command: ${command}`);
                    log.info('Run "node index.js help" for usage information');
                    process.exit(1);
                }
                break;
        }

        log.success('\\n✨ Done!');
        process.exit(0);

    } catch (error) {
        log.error('\\n❌ Operation failed!');
        if (config.logging.level === 'debug') {
            console.error(error);
        }
        process.exit(1);
    }
}

// Handle graceful shutdown
process.on('SIGINT', () => {
    log.warn('\\n⚠️  Interrupted by user');
    process.exit(1);
});

process.on('SIGTERM', () => {
    log.warn('\\n⚠️  Terminated');
    process.exit(1);
});

// Run main function
if (require.main === module) {
    main();
}