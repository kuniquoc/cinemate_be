# Tài liệu Cinemate

Thư mục `docs/` tập hợp hướng dẫn triển khai, vận hành và bộ công cụ kiểm thử cho Cinemate. Tất cả nội dung được tối giản để dễ đọc nhưng vẫn bao quát đầy đủ tính năng cần thiết.

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