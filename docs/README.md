# Movie Service Documentation

Thư mục này chứa tài liệu đầy đủ và công cụ test cho Movie Service API.

## 📚 Tài liệu

### [movie-service-api.md](./movie-service-api.md)
Tài liệu API đầy đủ bao gồm:
- Tổng quan về service
- Chi tiết tất cả endpoints
- Cấu trúc request/response
- Mã lỗi và xử lý lỗi
- Best practices cho direct upload và chunk upload
- Ví dụ cURL cho tất cả endpoints

## 🧪 Client Simulator

### [client-simulator/](./client-simulator/)
Bộ công cụ test và simulate client để test các API endpoints:

#### Nội dung chính:
- **Node.js scripts** để test từng loại upload
- **Postman collection** để test qua UI
- **Configuration** linh hoạt
- **Test data management**
- **Automated testing suite**

#### Quick Start:
```bash
cd client-simulator
npm install
# Thêm file video vào test-data/
run.bat test:all
```

## 🎯 Cách sử dụng

### 1. Đọc API Documentation
Bắt đầu với [movie-service-api.md](./movie-service-api.md) để hiểu:
- Các endpoint có sẵn
- Cách thức hoạt động của direct upload vs chunk upload
- Format của request/response
- Error handling

### 2. Setup Test Environment
```bash
# 1. Clone repo và start movie service
cd movie-service
mvn spring-boot:run

# 2. Setup client simulator
cd ../docs/client-simulator
npm install

# 3. Thêm test video files
# Xem hướng dẫn trong test-data/README.md
```

### 3. Test với Scripts
```bash
# Test upload trực tiếp
run.bat test:direct

# Test chunk upload
run.bat test:chunk

# Test status endpoints
run.bat test:status <movie-id>

# Chạy tất cả tests
run.bat test:all
```

### 4. Test với Postman
1. Import collection từ `client-simulator/postman/movie-service.postman_collection.json`
2. Set environment variable `baseUrl` = `http://localhost:8082`
3. Test từng endpoint theo thứ tự

## 🔍 Chi tiết các API

### Movie Management APIs
- **POST /api/movies/upload** - Upload trực tiếp (file nhỏ)
- **GET /api/movies/{id}/status** - Kiểm tra trạng thái xử lý
- **GET /api/movies/{id}** - Lấy thông tin chi tiết movie

### Chunk Upload APIs (cho file lớn)
- **POST /api/movies/chunk-upload/initiate** - Khởi tạo session
- **POST /api/movies/chunk-upload/{uploadId}/chunks/{chunkNumber}** - Upload chunk
- **GET /api/movies/chunk-upload/{uploadId}/status** - Kiểm tra tiến độ
- **POST /api/movies/chunk-upload/{uploadId}/complete** - Hoàn thành upload
- **DELETE /api/movies/chunk-upload/{uploadId}** - Hủy upload

### Utility APIs
- **GET /api/movies/chunk-upload/client.js** - JavaScript client code

## 🛠️ Tools và Utilities

### Client Simulator Features:
- ✅ **Automated testing** - Chạy test tự động cho tất cả endpoints
- ✅ **Progress tracking** - Hiển thị tiến độ upload với progress bar
- ✅ **Error handling** - Retry logic và error recovery
- ✅ **Configurable** - Tùy chỉnh chunk size, timeouts, v.v.
- ✅ **Cross-platform** - Windows batch script + Node.js
- ✅ **Postman integration** - Collection để test qua UI

### Supported Features:
- ✅ Direct file upload
- ✅ Chunked upload với concurrent chunks
- ✅ Upload progress monitoring
- ✅ Error retry và recovery
- ✅ File validation
- ✅ MD5 checksum verification
- ✅ Upload cancellation
- ✅ Movie status monitoring

## 📋 Requirements

### Server Requirements:
- Movie Service running trên port 8082
- PostgreSQL database
- MinIO storage service (cho file storage)
- FFmpeg (cho video transcoding)

### Client Requirements:
- Node.js 18+
- NPM packages (tự động install)
- Test video files

## 🎬 Workflow Examples

### Direct Upload Workflow:
1. Client upload file qua `/api/movies/upload`
2. Server lưu file và tạo Movie record
3. Background transcoding bắt đầu
4. Client poll `/api/movies/{id}/status` để check progress
5. Khi status = "READY", movie sẵn sàng streaming

### Chunk Upload Workflow:
1. Client initiate session qua `/api/movies/chunk-upload/initiate`
2. Client split file thành chunks và upload song song
3. Client check progress qua status endpoint
4. Khi tất cả chunks uploaded, client call complete endpoint
5. Server merge chunks và bắt đầu transcoding
6. Workflow tiếp tục như direct upload

## 🔧 Configuration

### Server Configuration:
```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 1GB
      max-request-size: 1GB

chunk-upload:
  max-file-size: 5GB
  min-chunk-size: 1MB
  max-chunk-size: 100MB
```

### Client Configuration:
```javascript
// config/config.js
module.exports = {
  api: {
    baseUrl: 'http://localhost:8082',
    timeout: 30000
  },
  upload: {
    chunkSize: 5 * 1024 * 1024, // 5MB
    maxConcurrentChunks: 3
  }
};
```

## 📞 Support

Nếu gặp vấn đề:
1. Check [QUICKSTART.md](./client-simulator/QUICKSTART.md) cho troubleshooting
2. Xem logs của Movie Service
3. Chạy với `--verbose` flag để có detailed logs
4. Check configuration files

## 🚀 Advanced Usage

### Custom Scripts:
Tạo custom test script bằng cách import utilities:
```javascript
const { createApiClient, log } = require('./scripts/utils');
const testDirectUpload = require('./scripts/direct-upload');

// Your custom test logic
```

### Integration Testing:
Sử dụng scripts trong CI/CD pipeline:
```bash
npm test  # Chạy automated test suite
```

### Performance Testing:
Modify config để test với different chunk sizes và concurrency levels.

---

**Tạo bởi**: PBL6 Team  
**Cập nhật**: September 2025