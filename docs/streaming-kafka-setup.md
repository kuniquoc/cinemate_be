# Hướng dẫn cấu hình Kafka cho dịch vụ Streaming

## 1. Mục tiêu
Tài liệu này hướng dẫn cách chuẩn bị hệ thống Kafka để dịch vụ `streaming` có thể tiêu thụ sự kiện fan-out cho các phiên xem phim, bao gồm việc đặt tên topic, tạo topic trên từng môi trường và cấu hình ACL/biến môi trường tương ứng.

## 2. Chuẩn bị
- Đã khởi tạo cụm Kafka/Zookeeper tương ứng từng môi trường (local, staging, production...).
- Có tài khoản quản trị Kafka (hoặc file `admin.properties`) đủ quyền tạo topic và ACL.
- Xác định `streamId` sẽ được tạo động bởi dịch vụ, từ đó cấu trúc topic sẽ là `<prefix><streamId>.events`.

## 3. Quy ước topic
- Mỗi môi trường dùng một `STREAMING_TOPIC_PREFIX` riêng nhằm tránh xung đột dữ liệu. Ví dụ:
  - Local: `stream.dev.`
  - Staging: `stream.stg.`
  - Production: `stream.`
- Topic hoàn chỉnh cho một stream cụ thể sẽ có dạng `stream.<streamId>.events` (ví dụ `stream.12345.events`).
- Số partition/replica đề xuất:
  - Local/dev: `1 partition`, `1 replica`.
  - Staging/prod: tối thiểu `3 partition`, `3 replica` (điều chỉnh theo kích cỡ cụm).

## 4. Tạo topic
### 4.1. Local (docker-compose)
1. Mở terminal và chạy lệnh tạo topic vào container broker:
   ```bash
   docker exec -it cinemate-broker kafka-topics \
     --bootstrap-server cinemate-broker:9092 \
     --create \
     --topic stream.demo.events \
     --partitions 1 \
     --replication-factor 1
   ```
2. Lặp lại bước trên với từng stream cố định hoặc bật chế độ auto-create topic nếu muốn để Kafka tự tạo khi có stream mới.

### 4.2. Môi trường staging/production
- Chạy lệnh từ một máy quản trị có kết nối vào Kafka cluster (đã cấu hình bảo mật trong `admin.properties`):
  ```bash
  kafka-topics \
    --bootstrap-server <broker-list> \
    --command-config admin.properties \
    --create \
    --topic stream.prod-demo.events \
    --partitions 3 \
    --replication-factor 3
  ```
- Quy trình tự động: tích hợp vào IaC hoặc pipeline để gọi lệnh tạo topic ngay khi ghi nhận `streamId` mới. Với môi trường cần tạo động liên tục, có thể giữ `auto.create.topics.enable=true` nhưng vẫn nên kiểm soát qua job định kỳ để đảm bảo số partition/replication đúng.

## 5. Cấp quyền ACL
Giả sử dịch vụ sử dụng principal `User:streaming-svc`.

```bash
# Cho phép đọc và mô tả tất cả topic có prefix stream.
kafka-acls \
  --bootstrap-server <broker-list> \
  --command-config admin.properties \
  --add \
  --allow-principal User:streaming-svc \
  --operation Read \
  --operation Describe \
  --topic stream. \
  --resource-pattern-type prefixed

# Cho phép consumer group commit offset
kafka-acls \
  --bootstrap-server <broker-list> \
  --command-config admin.properties \
  --add \
  --allow-principal User:streaming-svc \
  --operation Read \
  --group streaming-signaling
```

Nếu dịch vụ cần publish lên Kafka trong tương lai, bổ sung thêm `--operation Write` cho prefix tương ứng.

## 6. Biến môi trường bắt buộc
| Biến | Ý nghĩa | Ví dụ | Ghi chú |
| --- | --- | --- | --- |
| `KAFKA_BOOTSTRAP_SERVERS` | Danh sách broker mà dịch vụ kết nối | `cinemate-broker:9092` (local) / `broker-1:9092,broker-2:9092` | Bắt buộc |
| `KAFKA_CONSUMER_GROUP` | Consumer group để commit offset | `streaming-signaling` | Có thể tách theo môi trường nếu muốn |
| `STREAMING_TOPIC_PREFIX` | Prefix tạo topic và đăng ký subscriber | `stream.dev.` | Phải trùng với phần đã tạo topic/ACL |
| `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL` | Protocol bảo mật (nếu có) | `SASL_SSL` | Bỏ qua nếu cluster không yêu cầu |
| `SPRING_KAFKA_PROPERTIES_SASL_MECHANISM` | Cơ chế SASL | `PLAIN` / `SCRAM-SHA-512` | ... |
| `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` | Chuỗi JAAS chứa thông tin đăng nhập | `org.apache.kafka.common.security.plain.PlainLoginModule required username="streaming-svc" password="secret";` | Giữ bí mật qua secret manager |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis phục vụ lưu cache peer | `redis-host` | Không liên quan Kafka nhưng bắt buộc để dịch vụ hoạt động |

## 7. Kiểm tra sau cấu hình
1. Kiểm tra topic: `kafka-topics --bootstrap-server <broker-list> --describe --topic stream.demo.events`.
2. Kiểm tra ACL: `kafka-acls --bootstrap-server <broker-list> --list --principal User:streaming-svc`.
3. Khởi động dịch vụ `streaming` với các biến môi trường ở trên và quan sát log:
   - Log kỳ vọng: `Subscribed to Kafka topic stream.demo.events`.
   - Nếu gặp lỗi authorization, xem lại bước cấp ACL và thông tin SASL.

## 8. Ghi chú vận hành
- Chuẩn hóa việc tạo topic/ACL bằng script hoặc Terraform để tránh thao tác thủ công.
- Định kỳ rà soát để xóa topic không còn sử dụng, tránh rò rỉ dữ liệu.
- Lưu ý giới hạn quota của Kafka (throttling) nếu tạo số lượng lớn topic động; có thể gom stream ít hoạt động dùng chung topic nếu cần.
