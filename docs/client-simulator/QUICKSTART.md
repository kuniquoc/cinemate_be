# Movie Service Client Simulator - Quick Start Guide

## Setup Instructions

### 1. Prerequisites
- Node.js 18+ installed
- Movie Service running on http://localhost:8082
- Test video files

### 2. Installation
```bash
cd docs/client-simulator
npm install
```

### 3. Prepare Test Data
Place video files in the `test-data/` directory:
- `sample-video.mp4` - for direct upload testing (< 100MB)
- `large-video.mp4` - for chunk upload testing (> 100MB)

### 4. Configuration
Edit `config/config.js` to customize:
- API endpoint URL
- Chunk size
- Retry settings
- Test data paths

## Quick Commands

### Windows (Using batch file):
```batch
# Show help
run.bat

# Test direct upload
run.bat test:direct

# Test chunk upload
run.bat test:chunk

# Test movie status
run.bat test:status <movie-id>

# Monitor movie processing
run.bat test:monitor <movie-id>

# Run all tests
run.bat test:all
```

### Cross-platform (Using Node.js):
```bash
# Show help
node index.js help

# Test direct upload
node index.js test:direct

# Test chunk upload
node index.js test:chunk

# Test movie status
node index.js test:status <movie-id>

# Monitor movie processing
node index.js test:monitor <movie-id>

# Run all tests
node index.js test:all
```

### Using NPM scripts:
```bash
# Test direct upload
npm run test:direct

# Test chunk upload
npm run test:chunk

# Run all tests
npm run test:all
```

## Testing Flow

1. **Start Movie Service** - Make sure it's running on port 8082
2. **Prepare Test Files** - Add video files to test-data/
3. **Run Direct Upload Test** - `run.bat test:direct`
4. **Run Chunk Upload Test** - `run.bat test:chunk`
5. **Monitor Processing** - `run.bat test:monitor <movie-id>`

## Using Postman

1. Import the collection: `postman/movie-service.postman_collection.json`
2. Set environment variables:
   - `baseUrl`: http://localhost:8082
3. Run requests in order:
   - Upload Movie or Initiate Chunk Upload
   - Check Movie Status
   - Get Movie Info

## Troubleshooting

### Common Issues:

**"Connection refused"**
- Make sure Movie Service is running on port 8082
- Check if the port is correct in config.js

**"Test video file not found"**
- Add video files to test-data/ directory
- Check file paths in config.js

**"Upload failed"**
- Check file size limits
- Verify file format is supported
- Check server logs for details

**"Dependencies not installed"**
- Run `npm install` in the client-simulator directory
- Make sure Node.js is installed

### Debug Mode:
Add `--verbose` flag for detailed logging:
```bash
run.bat test:all --verbose
```

## File Structure Reference
```
client-simulator/
├── index.js              # Main entry point
├── run.bat               # Windows batch script
├── package.json          # Dependencies
├── config/
│   └── config.js         # Configuration
├── scripts/
│   ├── utils.js          # Common utilities
│   ├── direct-upload.js  # Direct upload test
│   ├── chunk-upload.js   # Chunk upload test
│   └── movie-status.js   # Status checking
├── test-data/
│   ├── README.md         # Test data instructions
│   ├── sample-video.mp4  # Your test video (small)
│   └── large-video.mp4   # Your test video (large)
└── postman/
    └── movie-service.postman_collection.json
```