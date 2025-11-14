# Documentation Index

This directory contains comprehensive documentation for the Cinemate backend services.

## Service Documentation

### Streaming Services
- **[Streaming Signaling Protocol](streaming-signaling-protocol.md)** - Chi tiết đầy đủ về WebSocket signaling protocol cho P2P streaming
- **[Streaming Signaling Messages - Quick Reference](streaming-signaling-messages.md)** - Tổng hợp nhanh các message types
- **[Streaming Deployment Guide](streaming-deployment-guide.md)** - Hướng dẫn deploy streaming services
- **[Streaming Kafka Setup](streaming-kafka-setup.md)** - Cấu hình Kafka cho streaming

### Migration Guides
- **[Message Type Migration](MIGRATION.md)** - Chi tiết migration từ UPPER_CASE sang camelCase

### Agent Service
- **[Agent Service Guide](agent-service-guide.md)** - Hướng dẫn agent service

## Quick Links

### Streaming Signaling Protocol
- **Connection**: `ws://<host>:8083/ws/signaling?clientId=<id>&streamId=<movieId>`
- **Message Types**: `whoHas`, `reportSegment`, `rtcOffer`, `rtcAnswer`, `iceCandidate`, `peerList`, `whoHasReply`, `reportAck`, `error`
- **Segment ID Format**: `seg_XXXX.m4s` (bao gồm extension, ví dụ: `seg_0005.m4s`)
- **Full Spec**: [streaming-signaling-protocol.md](streaming-signaling-protocol.md)

### Recent Updates

#### 2025-11-14: Segment ID Format Unification
`segmentId` giờ đây bao gồm file extension (ví dụ: `seg_0005.m4s`) thay vì chỉ là identifier không có extension (`seg_0005`). Điều này đơn giản hóa logic xử lý vì `segmentId` chính là filename đầy đủ.

#### 2025-11-14: Message Type Migration
Tất cả message types trong Streaming Signaling đã được chuyển từ UPPER_CASE sang camelCase:
- `WHO_HAS` → `whoHas`
- `REPORT_SEGMENT` → `reportSegment`
- `WHO_HAS_REPLY` → `whoHasReply`
- `REPORT_ACK` → `reportAck`
- `RTC_OFFER` → `rtcOffer`
- `RTC_ANSWER` → `rtcAnswer`
- `ICE_CANDIDATE` → `iceCandidate`
- `peer_list` → `peerList`
- `ERROR` → `error`

See [MIGRATION.md](MIGRATION.md) for details.

## Documentation Structure

```
docs/
├── README.md                              # This file
├── MIGRATION.md                           # Migration guide
├── agent-service-guide.md                 # Agent service documentation
├── streaming-signaling-protocol.md        # Full protocol specification
├── streaming-signaling-messages.md        # Message quick reference
├── streaming-deployment-guide.md          # Deployment guide
└── streaming-kafka-setup.md               # Kafka configuration
```

## Contributing to Documentation

When adding new documentation:
1. Place files in the `docs/` directory
2. Use Markdown format
3. Update this README.md with links
4. Include examples and use cases
5. Keep technical specifications detailed but readable Tất cả nội dung được tối giản để dễ đọc nhưng vẫn bao quát đầy đủ tính năng cần thiết.

## 📂 Danh mục nhanh

| Chủ đề                                                           | Nội dung                                                                | Đối tượng |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------- | --------- |
| [client-simulator](./client-simulator/README.md)                 | Bộ mô phỏng client kiểm thử Movie Service (Node.js, Postman, test data) | QA, Dev   |
| [streaming-deployment-guide.md](./streaming-deployment-guide.md) | Quy trình triển khai signaling và seeder                                | DevOps    |
| [streaming-kafka-setup.md](./streaming-kafka-setup.md)           | Chuẩn bị Kafka topic cho streaming                                      | DevOps    |
| [agent-service-guide.md](./agent-service-guide.md)               | Playbook tạo service mới trong monorepo                                 | Dev       |

## � Quy trình gợi ý

1. **Nắm kiến trúc**: đọc nhanh các guide về streaming và agent service để hiểu chuẩn cấu hình.
2. **Khởi động Movie Service**: chạy `docker-compose up movie-service` hoặc `mvn spring-boot:run` tại module `movie-service`.
3. **Chuẩn bị bộ mô phỏng**: theo hướng dẫn trong `client-simulator/README.md` để cài Node.js, cấu hình `config.js` và đặt dữ liệu mẫu.
4. **Thực thi kiểm thử**: dùng `run.bat` (Windows) hoặc `node index.js <command>` để kiểm tra upload trực tiếp, upload theo chunk, giám sát trạng thái.
5. **Theo dõi kết quả**: ghi lại movieId, thời gian upload và log lỗi (nếu có) cho mỗi phiên test.

## 🧪 Bộ mô phỏng client (Movie Service)

- Được tối ưu thành các script độc lập, dễ bảo trì.
- Cho phép tùy biến endpoint, timeout, kích thước chunk thông qua biến môi trường:
  - `MOVIE_SERVICE_BASE_URL`, `MOVIE_SERVICE_TIMEOUT_MS`
  - `MOVIE_UPLOAD_CHUNK_MB`, `MOVIE_UPLOAD_CONCURRENCY`
- Có thể chạy trực tiếp qua `npm run` hoặc CLI `node index.js test:all`.
- Postman collection nằm trong `client-simulator/postman/` cho trường hợp cần thao tác thủ công.

> Chi tiết thiết lập, sơ đồ thư mục và câu lệnh tham khảo xem tại `client-simulator/README.md` và `client-simulator/QUICKSTART.md`.

## 🧭 Tips chung

- Ưu tiên chạy script ở chế độ `--verbose` khi cần điều tra lỗi mạng hoặc timeout.
- Lưu movieId trả về sau upload để thuận tiện kiểm tra trạng thái về sau.
- Ghi chú thông số chunk (kích thước, số luồng) đã dùng khi thực hiện các phép đo hiệu năng.

---

**Người duy trì**: Nhóm PBL6  
**Cập nhật lần cuối**: Tháng 11/2025