const config = require('../config/config');
const { createApiClient, log, handleApiError } = require('./utils');

async function testMovieStatus(movieId) {
    log.info('🎬 Testing Movie Status Endpoints');
    log.info('=================================');

    if (!movieId) {
        log.error('Movie ID is required');
        log.info('Usage: node movie-status.js <movieId>');
        return;
    }

    const api = createApiClient();

    try {
        // Test movie status endpoint
        log.info(`\\n1. Getting movie status for ID: ${movieId}`);

        const statusResponse = await api.get(`/api/movies/${movieId}/status`);

        log.success('Movie status retrieved successfully');
        // MovieController now uses ResponseData wrapper
        log.info(`Movie ID: ${statusResponse.data.data.movieId}`);
        log.info(`Status: ${statusResponse.data.data.status}`);

        if (statusResponse.data.data.qualities && Object.keys(statusResponse.data.data.qualities).length > 0) {
            log.info('Available qualities:');
            Object.entries(statusResponse.data.data.qualities).forEach(([quality, url]) => {
                log.info(`  ${quality}: ${url}`);
            });
        } else {
            log.info('No transcoded qualities available yet');
        }

        // Test movie info endpoint
        log.info(`\\n2. Getting detailed movie info for ID: ${movieId}`);

        const infoResponse = await api.get(`/api/movies/${movieId}`);

        log.success('Movie info retrieved successfully');
        log.info(`Movie ID: ${infoResponse.data.data.movieId}`);
        log.info(`Title: ${infoResponse.data.data.title}`);
        log.info(`Description: ${infoResponse.data.data.description}`);
        log.info(`Status: ${infoResponse.data.data.status}`);

        if (infoResponse.data.data.qualities && Object.keys(infoResponse.data.data.qualities).length > 0) {
            log.info('Available qualities:');
            Object.entries(infoResponse.data.data.qualities).forEach(([quality, url]) => {
                log.info(`  ${quality}: ${url}`);
            });
        } else {
            log.info('No transcoded qualities available yet');
        }

        // Status interpretation
        log.info('\\n📊 Status Interpretation:');
        interpretStatus(infoResponse.data.data.status);

        log.success('\\n✨ Movie status test completed successfully!');

        return {
            movieId: infoResponse.data.data.movieId,
            title: infoResponse.data.data.title,
            status: infoResponse.data.data.status,
            qualitiesCount: infoResponse.data.data.qualities ? Object.keys(infoResponse.data.data.qualities).length : 0
        };

    } catch (error) {
        handleApiError(error, 'Movie status check');
        throw error;
    }
}

function interpretStatus(status) {
    switch (status) {
        case 'PENDING':
            log.info('🔄 Movie is pending - waiting to be processed');
            log.info('   The movie has been uploaded but processing hasn\'t started yet');
            break;

        case 'PROCESSING':
            log.info('⚙️  Movie is being processed - transcoding in progress');
            log.info('   The video is being converted to different qualities');
            log.info('   This may take several minutes depending on file size');
            break;

        case 'READY':
            log.info('✅ Movie is ready - processing completed successfully');
            log.info('   The video has been transcoded and is available for streaming');
            break;

        case 'FAILED':
            log.error('❌ Movie processing failed');
            log.info('   There was an error during transcoding');
            log.info('   Check the logs or try uploading again');
            break;

        default:
            log.warn(`⚠️  Unknown status: ${status}`);
            break;
    }
}

async function monitorMovieStatus(movieId, maxAttempts = 30, intervalSeconds = 10) {
    log.info('🔍 Starting movie status monitoring...');
    log.info(`Will check every ${intervalSeconds} seconds for up to ${maxAttempts} attempts`);

    const api = createApiClient();

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            const response = await api.get(`/api/movies/${movieId}/status`);
            const status = response.data.status;

            log.info(`[${attempt}/${maxAttempts}] Current status: ${status}`);

            if (status === 'READY') {
                log.success('🎉 Movie is ready!');

                // Get final movie info
                const infoResponse = await api.get(`/api/movies/${movieId}`);

                if (infoResponse.data.qualities) {
                    log.info('Available qualities:');
                    Object.keys(infoResponse.data.qualities).forEach(quality => {
                        log.info(`  ✓ ${quality}`);
                    });
                }

                return infoResponse.data;
            } else if (status === 'FAILED') {
                log.error('❌ Movie processing failed');
                throw new Error('Movie processing failed');
            }

            if (attempt < maxAttempts) {
                log.info(`Waiting ${intervalSeconds} seconds before next check...`);
                await new Promise(resolve => setTimeout(resolve, intervalSeconds * 1000));
            }

        } catch (error) {
            handleApiError(error, `Status check attempt ${attempt}`);

            if (attempt < maxAttempts) {
                log.info('Retrying in 5 seconds...');
                await new Promise(resolve => setTimeout(resolve, 5000));
            } else {
                throw error;
            }
        }
    }

    log.warn('⏰ Monitoring timeout reached');
    log.info('Movie may still be processing. Check manually later.');

    return null;
}

// Run the test if called directly
if (require.main === module) {
    const movieId = process.argv[2];
    const shouldMonitor = process.argv.includes('--monitor');

    if (shouldMonitor && movieId) {
        monitorMovieStatus(movieId)
            .then(() => {
                log.success('Monitoring completed!');
                process.exit(0);
            })
            .catch(() => {
                log.error('Monitoring failed!');
                process.exit(1);
            });
    } else {
        testMovieStatus(movieId)
            .then(() => {
                log.success('Test completed successfully!');
                process.exit(0);
            })
            .catch(() => {
                log.error('Test failed!');
                process.exit(1);
            });
    }
}

module.exports = { testMovieStatus, monitorMovieStatus };