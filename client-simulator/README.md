# Bộ mô phỏng client Movie Service

Mục tiêu của thư mục này là cung cấp bộ script gọn nhẹ để kiểm thử toàn bộ luồng upload phim (trực tiếp và chunk), giám sát trạng thái xử lý và mô phỏng viewer streaming.

## 📁 Cấu trúc thư mục

```
client-simulator/
├── README.md
├── QUICKSTART.md
├── package.json
├── run.bat
├── config/
│   └── config.js              # Tùy chỉnh endpoint, timeout, chunk size
├── scripts/
│   ├── direct-upload.js       # Kiểm thử upload trực tiếp
│   ├── chunk-upload.js        # Kiểm thử upload theo chunk
│   ├── movie-status.js        # Kiểm tra và theo dõi trạng thái phim
│   ├── run-all-tests.js       # Gộp các kịch bản kiểm thử chính
│   ├── utils.js               # Tiện ích chung (retry, log, format)
│   ├── lib/
│   │   └── chunk-uploader.js  # Lớp điều khiển upload chunk tái sử dụng
│   └── streaming/
│       └── ...                # Bộ mô phỏng viewer P2P
├── postman/
│   └── movie-service.postman_collection.json
└── test-data/
    ├── README.md
    └── *.mp4 (tự cung cấp)
```

## ⚙️ Chuẩn bị môi trường

1. **Cài phụ thuộc**
   ```bash
   cd docs/client-simulator
   npm install
   ```
2. **Đặt file test**: thêm `sample-video.mp4` (nhỏ) và `large-video.mp4` (lớn) vào `test-data/`. Nếu chưa có, tạo nhanh bằng `ffmpeg` theo hướng dẫn trong `test-data/README.md`.
3. **Chỉnh cấu hình**: mở `config/config.js` để kiểm tra URL backend. Có thể override bằng biến môi trường:
   - `MOVIE_SERVICE_BASE_URL` (mặc định `http://localhost:8080`)
   - `MOVIE_SERVICE_TIMEOUT_MS`
   - `MOVIE_UPLOAD_CHUNK_MB`
   - `MOVIE_UPLOAD_CONCURRENCY`

## 🚀 Các cách chạy phổ biến

| Nhu cầu                   | Câu lệnh                                               | Ghi chú                                            |
| ------------------------- | ------------------------------------------------------ | -------------------------------------------------- |
| Xem trợ giúp nhanh        | `node index.js help` hoặc chạy `run.bat` không tham số | In danh sách lệnh hỗ trợ                           |
| Kiểm thử upload trực tiếp | `node index.js test:direct`                            | Dùng file nhỏ (`sample-video.mp4`)                 |
| Kiểm thử upload chunk     | `node index.js test:chunk`                             | Ưu tiên file lớn, fallback sang file nhỏ nếu thiếu |
| Kiểm tra trạng thái phim  | `node index.js test:status <movieId>`                  | Trả về metadata và các chất lượng đã xử lý         |
| Theo dõi tới khi hoàn tất | `node index.js test:monitor <movieId>`                 | Poll liên tục cho đến READY/FAILED                 |
| Chạy toàn bộ kịch bản     | `node index.js test:all`                               | Bao gồm kiểm tra kết nối + upload                  |

- Trên Windows có thể dùng `run.bat test:chunk`, `run.bat test:all` để tự động kiểm tra Node.js và cài phụ thuộc.
- Khi cần log chi tiết: thêm `--verbose` sau câu lệnh (ví dụ `node index.js test:all --verbose`).

## 📝 Ghi chú vận hành

- **Nhật ký output** đã được chuẩn hóa (timestamp, icon) giúp nhận diện lỗi nhanh.
- Script chunk upload nay dùng lớp `ChunkUploader` riêng, giúp tái sử dụng trong automation khác nếu cần require trực tiếp từ `scripts/lib/chunk-uploader.js`.
- `run-all-tests.js` chịu trách nhiệm kiểm tra kết nối API trước khi thực thi, tránh mất thời gian chờ request thất bại.
- Postman collection có sẵn biến `baseUrl`; cập nhật giá trị này nếu Movie Service chạy trên port khác 8080/8081.

## 📦 Streaming playback simulator

- Sử dụng `npm run streaming:playback -- --stream=<id>` để mô phỏng viewer.
- Tham số hữu ích: `--stream=<movieId>_<quality>`, `--manifest=<path>`, `--client=<id>`.
- Viewer mặc định tự gọi Movie Service để lấy URL manifest và tải `index.m3u8` từ origin; chỉ cần cung cấp manifest thủ công khi kiểm thử ngoại tuyến. Origin trong cấu hình chỉ phục vụ mục đích tải manifest.
- Các tham số liên quan tới buffer, retry, fallback HTTP nằm trong `config.streaming`.

### Lấy `streamId` ở đâu?

- `streamId` được chuẩn hoá thành `<movieId>_<quality>` (ví dụ `2d3c...-9a72_720p`). Seeder, Redis và viewer đều dùng chung định dạng này.
- Cách tra cứu nhanh:
   - Gọi `node index.js test:status <movieId>` (hoặc `movie-status.js`) để nhận map `qualities`. Chọn một quality (`360p`, `720p`, ...) rồi ghép với `movieId` bằng dấu gạch dưới.
   - Xem log `streaming-seeder`: khi đăng ký cache mới sẽ có dòng `Registered ... for stream <movieId>_<quality>`.
   - Trực tiếp liệt kê thư mục cache: `docker exec <container-seeder> ls /var/cinemate/cache`.
- Đảm bảo streamId bạn truyền cho viewer và signaling đúng với quality mong muốn; mỗi quality tương ứng một stream độc lập.

### Chuỗi lấy segment: P2P ➡ Seeder ➡ Origin

1. **P2P trước hết:** Mỗi viewer khởi tạo kết nối WebSocket tới `config.streaming.signaling.wsBaseUrl` với query `clientId` và `streamId`. Signaling sẽ trả về danh sách peer đang giữ segment. Script sẽ ưu tiên tải từ các peer này (theo điểm số `peer-selector.js`).
2. **Fallback sang Seeder:** Nếu tất cả peer đều không có hoặc trả lỗi, viewer gọi `GET {seederBaseUrl}/streams/{streamId}/segments/{segmentId}`. Đảm bảo `config.streaming.fallback.seeder.baseUrl` (hoặc `STREAMING_SEEDER_BASE_URL`) trỏ đúng tới cổng 8084 của dịch vụ seeder.
3. **Seeder tự lấy từ Origin (MinIO):** Khi seeder chưa có file, nó sẽ tự tải `segmentId` tương ứng từ MinIO (dựa trên `minio.*` và `streaming.seeder.origin.*`). Không cần thủ công chép file như trước; lần gọi đầu tiên có thể chậm hơn vì phải copy từ origin.
4. **Seeder đồng bộ origin:** Khi chưa có segment, seeder tự động kéo từ MinIO theo cấu hình `streaming.seeder.origin.*`, ghi vào cache rồi trả về cho viewer. Viewer không truy cập origin trực tiếp.
5. **Cache cục bộ viewer:** Với `persistCacheToDisk=true`, mỗi viewer cũng lưu segment xuống `config.streaming.fallback.cacheDirectory`. Điều này giúp cùng một máy xem lại nhanh hơn nhưng không thay thế seeder.

## ✅ Checklist nhanh trước khi test

- [ ] Movie Service đang chạy và truy cập được từ máy cục bộ.
- [ ] Đã cài phụ thuộc (`node_modules/`).
- [ ] File video mẫu tồn tại trong `test-data/`.
- [ ] Đã đặt đúng giá trị `MOVIE_SERVICE_BASE_URL` nếu chạy qua Docker (thường là `http://localhost:8081`).
- [ ] Có đủ dung lượng đĩa để lưu file tải lên và cache streaming.

Tham khảo thêm quy trình thao tác chi tiết trong `QUICKSTART.md`.