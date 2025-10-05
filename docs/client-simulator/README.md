# Movie Service Client Simulator

Thư mục này chứa các tool và script để test các endpoint của Movie Service.

## Structure

```
client-simulator/
├── README.md                   # This file
├── package.json               # Node.js dependencies
├── config/
│   └── config.js              # Configuration settings
├── scripts/
│   ├── direct-upload.js       # Test direct movie upload
│   ├── chunk-upload.js        # Test chunk upload
│   ├── movie-status.js        # Test movie status endpoints
│   └── utils.js               # Common utilities
├── test-data/
│   ├── sample-video.mp4       # Small sample video (place your own)
│   └── large-video.mp4        # Large sample video (place your own)
└── postman/
    └── movie-service.postman_collection.json  # Postman collection
```

## Setup

1. Install Node.js dependencies:
```bash
cd client-simulator
npm install
```

2. Configure settings in `config/config.js`

3. Place test video files in `test-data/` folder

## Usage

### Run All Tests:
```bash
node scripts/run-all-tests.js
```

### Test API Connection:
```bash
node scripts/api-test.js
```

### Test Direct Upload:
```bash
node scripts/direct-upload.js
```

### Test Chunk Upload:
```bash
node scripts/chunk-upload.js
```

### Test Movie Status:
```bash
node scripts/movie-status.js <movieId>
```

## Requirements

- Node.js 18+
- Movie Service running on http://localhost:8082
- Test video files in test-data/ folder