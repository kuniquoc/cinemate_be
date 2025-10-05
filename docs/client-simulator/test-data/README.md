# Test Data Directory

This directory should contain your test video files for testing the Movie Service API.

## Required Files:

### sample-video.mp4
- A small video file (< 100MB) for testing direct upload
- Can be any common video format (MP4, AVI, MOV, MKV, WebM)
- Example sources:
  - Record a short video with your phone
  - Download a sample video from: https://sample-videos.com/
  - Use ffmpeg to create a test video: `ffmpeg -f lavfi -i testsrc=duration=30:size=640x480:rate=30 sample-video.mp4`

### large-video.mp4 (Optional)
- A larger video file (> 100MB) for testing chunk upload
- Useful for testing the chunked upload functionality
- Can be the same formats as above, just larger

## Creating Test Videos with FFmpeg:

If you have FFmpeg installed, you can create test videos:

```bash
# Create a 30-second test video (small, ~5MB)
ffmpeg -f lavfi -i testsrc=duration=30:size=640x480:rate=30 sample-video.mp4

# Create a 5-minute test video (larger, ~50MB)
ffmpeg -f lavfi -i testsrc=duration=300:size=1280x720:rate=30 large-video.mp4

# Create a very large test video for chunk upload testing (~200MB)
ffmpeg -f lavfi -i testsrc=duration=600:size=1920x1080:rate=30 large-video.mp4
```

## File Naming:
- Keep the exact filenames `sample-video.mp4` and `large-video.mp4` as they are referenced in the configuration
- Or update the paths in `../config/config.js` if you use different names

## Supported Formats:
- MP4 (recommended)
- AVI
- MOV
- MKV
- WebM

**Note**: The actual test files are not included in the repository to keep it lightweight. You need to provide your own test videos.