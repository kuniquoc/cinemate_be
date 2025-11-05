# Hướng dẫn nhanh bộ mô phỏng client

## 1. Điều kiện tiên quyết

- Node.js 18 trở lên
- Movie Service đang chạy và truy cập được (mặc định `http://localhost:8080`, trong Docker thường là `http://localhost:8081`)
- Có ít nhất 1 file video mẫu

## 2. Cài đặt và cấu hình

```bash
cd docs/client-simulator
npm install
```

- Thêm video vào `test-data/`:
   - `sample-video.mp4`: dùng cho upload trực tiếp
   - `large-video.mp4`: dùng cho upload chunk
- Kiểm tra `config/config.js`. Có thể override bằng biến môi trường trước khi chạy lệnh, ví dụ:
   ```bash
   set MOVIE_SERVICE_BASE_URL=http://localhost:8081
   set MOVIE_UPLOAD_CHUNK_MB=10
   ```

## 3. Thao tác nhanh

### Windows (`run.bat`)
```bat
run.bat                  rem Hiển thị hướng dẫn
run.bat test:direct      rem Upload trực tiếp
run.bat test:chunk       rem Upload chunk
run.bat test:status <id> rem Kiểm tra trạng thái
run.bat test:monitor <id>rem Theo dõi tới READY
run.bat test:all         rem Chạy trọn bộ kiểm thử
```

### Nền tảng bất kỳ (Node CLI)
```bash
node index.js help
node index.js test:direct
node index.js test:chunk
node index.js test:status <movieId>
node index.js test:monitor <movieId>
node index.js test:all
```

### NPM scripts rút gọn
```bash
npm run test:direct
npm run test:chunk
npm run test:monitor -- <movieId>
npm run test:all
npm run streaming:playback -- --stream=<movieId>_720p --segments=12
```

## 4. Quy trình khuyến nghị

1. Khởi động Movie Service và xác nhận `/actuator/health` hoặc endpoint bất kỳ trả về 200.
2. Chạy `node index.js test:direct` để đảm bảo luồng đơn giản hoạt động.
3. Thực hiện `node index.js test:chunk` với file lớn, lưu lại `movieId`.
4. Dùng `node index.js test:monitor <movieId>` để theo dõi đến khi trạng thái `READY`.
5. (Tuỳ chọn) Mở Postman collection `postman/movie-service.postman_collection.json`, thiết lập `baseUrl` và gọi lại các endpoint nếu cần thao tác thủ công.

## 5. Xử lý sự cố nhanh

| Triệu chứng                | Cách khắc phục                                                                                 |
| -------------------------- | ---------------------------------------------------------------------------------------------- |
| `ECONNREFUSED`             | Kiểm tra Movie Service đã chạy, đúng cổng chưa. Xác nhận cấu hình `MOVIE_SERVICE_BASE_URL`.    |
| Thiếu video mẫu            | Tạo nhanh bằng `ffmpeg -f lavfi -i testsrc=duration=30:size=640x480:rate=30 sample-video.mp4`. |
| Upload thất bại giữa chừng | Kiểm tra log để biết chunk nào lỗi, xem lại giới hạn kích thước hoặc mạng.                     |
| Không có `node_modules`    | Chạy lại `npm install`.                                                                        |

> Khi cần log chi tiết hơn, thêm `--verbose` vào câu lệnh (ví dụ `node index.js test:all --verbose`).

## 6. Tham chiếu cấu trúc

```
client-simulator/
├── index.js
├── run.bat
├── package.json
├── config/
│   └── config.js
├── scripts/
│   ├── direct-upload.js
│   ├── chunk-upload.js
│   ├── movie-status.js
│   ├── run-all-tests.js
│   ├── utils.js
│   ├── lib/
│   │   └── chunk-uploader.js
│   └── streaming/
│       └── viewer.js (cùng các tiện ích liên quan)
└── test-data/
      ├── README.md
      └── *.mp4
```

## 7. Phát streaming: P2P → Seeder → Origin

1. **Xác định `streamId`:**
   - Gọi `node index.js test:status <movieId>` để lấy danh sách quality; ghép `streamId=<movieId>_<quality>`.
   - Hoặc đọc log `streaming-seeder`/`streaming-signaling` để thấy định danh được đăng ký (`Registered … for stream <movieId>_<quality>`).
   - Có thể kiểm tra nhanh bằng `docker exec <tên-container-seeder> ls /var/cinemate/cache`.
2. **Chạy viewer ưu tiên P2P:**
   ```bash
   npm run streaming:playback -- --stream=<movieId>_<quality> --segments=12
   ```
   Start ít nhất hai viewer (máy/terminal khác nhau) cùng `streamId` để nhìn thấy luồng P2P thực sự.
3. **Cấu hình fallback sang seeder:** đảm bảo `config.streaming.fallback.seeder.baseUrl` (hoặc biến môi trường `STREAMING_SEEDER_BASE_URL`) trỏ về `http://<host>:8084`.
4. **Seeder tự đồng bộ MinIO:** lần đầu yêu cầu một segment, seeder sẽ tải từ MinIO dựa trên `minio.*` + `streaming.seeder.origin.*`. Theo dõi log `streaming-seeder` để thấy dòng `Fetched segment ... from origin`.
5. **Bật fallback origin cho viewer:** đặt `config.streaming.fallback.origin.baseUrl` (mặc định `http://localhost:9000/movies`). Viewer sẽ gọi trực tiếp origin nếu seeder không trả được, đồng thời lưu cache tại `config.streaming.fallback.cacheDirectory`.