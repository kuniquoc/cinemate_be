const config = require('../config/config');
const { createApiClient, log } = require('./utils');

async function testApiConnection() {
    log.info('🔗 Testing API Connection');
    log.info('========================');

    const api = createApiClient();

    try {
        // Test basic connectivity by trying to get JavaScript client
        log.info('Testing basic API connectivity...');

        const response = await api.get('/api/movies/chunk-upload/client.js');

        if (response.status === 200) {
            log.success('✅ API connection successful!');
            log.info(`Base URL: ${config.api.baseUrl}`);
            log.info(`Response: ${response.status} ${response.statusText}`);
            log.info(`Content length: ${response.data.length} characters`);
            return true;
        }

    } catch (error) {
        log.error('❌ API connection failed!');
        log.error(`Base URL: ${config.api.baseUrl}`);

        if (error.code === 'ECONNREFUSED') {
            log.error('Connection refused - is the movie service running?');
            log.info('Try starting the service with: mvn spring-boot:run');
        } else if (error.response) {
            log.error(`HTTP ${error.response.status}: ${error.response.statusText}`);
        } else {
            log.error(`Error: ${error.message}`);
        }

        return false;
    }
}

// Test various endpoints
async function testEndpoints() {
    log.info('\n🧪 Testing API Endpoints');
    log.info('========================');

    const api = createApiClient();
    const endpoints = [
        {
            name: 'Get JavaScript Client',
            method: 'GET',
            path: '/api/movies/chunk-upload/client.js',
            expectedStatus: 200
        }
    ];

    for (const endpoint of endpoints) {
        try {
            log.info(`Testing ${endpoint.method} ${endpoint.path}...`);

            const response = await api({
                method: endpoint.method,
                url: endpoint.path
            });

            if (response.status === endpoint.expectedStatus) {
                log.success(`✅ ${endpoint.name}: OK`);
            } else {
                log.warn(`⚠️  ${endpoint.name}: Unexpected status ${response.status}`);
            }

        } catch (error) {
            if (error.response && error.response.status === 404) {
                log.error(`❌ ${endpoint.name}: Endpoint not found (404)`);
            } else {
                log.error(`❌ ${endpoint.name}: ${error.message}`);
            }
        }
    }
}

async function runTests() {
    try {
        const connected = await testApiConnection();

        if (connected) {
            await testEndpoints();
            log.success('\n✨ API tests completed!');
        } else {
            log.error('\n❌ Cannot proceed with endpoint tests - API connection failed');
            process.exit(1);
        }

    } catch (error) {
        log.error('Test suite failed:', error.message);
        process.exit(1);
    }
}

// Run tests if called directly
if (require.main === module) {
    runTests();
}

module.exports = { testApiConnection, testEndpoints };