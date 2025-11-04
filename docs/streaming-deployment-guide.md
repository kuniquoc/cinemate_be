# Streaming Deployment Guide (Signaling & Seeder)

## 1. Kiến trúc tách hai container
- **streaming-signaling**: cung cấp endpoint WebSocket `/ws/signaling`, subscribe Kafka topic sự kiện stream và cập nhật danh sách peer hoạt động vào Redis. Service không phụ thuộc filesystem cache nên scale ngang theo lượng viewer.
- **streaming-seeder**: quét thư mục cache, đồng bộ metadata segment/peer vào Redis và dọn dẹp file hết hạn. Không kết nối Kafka, tập trung vào I/O và scheduler nên scale theo số node lưu cache.
- Cả hai chia sẻ Redis và cùng sử dụng helper `StreamingRedisKeys` trong `shared-kernel` để thống nhất key. Việc tách container giúp cấu hình tài nguyên chuyên biệt (CPU/network cho signaling, disk/io cho seeder).

## 2. Chuẩn bị build image
1. Biên dịch cả hai module mới:
   ```powershell
   mvn -pl streaming-signaling,streaming-seeder -am clean package
   ```
2. Build image cho signaling từ thư mục gốc repo (Dockerfile đã tham chiếu parent POM):
   ```powershell
   docker build -t cinemate/streaming-signaling:latest -f streaming-signaling/Dockerfile .
   ```
3. Build image cho seeder:
   ```powershell
   docker build -t cinemate/streaming-seeder:latest -f streaming-seeder/Dockerfile .
   ```
4. Dockerfile đã expose port (`8083` cho signaling, `8084` cho seeder) và nhận `JAVA_OPTS`. Khi cần thay đổi port, đặt biến `SERVER_PORT` lúc khởi chạy container.

## 3. Biến môi trường cốt lõi
| Biến                                           | streaming-signaling       | streaming-seeder                | Ghi chú                                  |
| ---------------------------------------------- | ------------------------- | ------------------------------- | ---------------------------------------- |
| `SERVER_PORT`                                  | Mặc định `8083`           | Mặc định `8084`                 | Đổi nếu chạy trên cùng host.             |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | ✅                         | ✅                               | Bắt buộc để ghi nhận state peer/segment. |
| `KAFKA_BOOTSTRAP_SERVERS`                      | ✅                         | ✖                               | Signaling subscribe sự kiện stream.      |
| `KAFKA_CONSUMER_GROUP`                         | Ví dụ `signaling-cluster` | ✖                               | Tách offset theo cụm signaling.          |
| `STREAMING_TOPIC_PREFIX`                       | Ví dụ `stream.`           | ✖                               | Phải khớp với prefix đã tạo topic.       |
| `STREAMING_MAX_ACTIVE_PEERS`                   | Tuỳ chỉnh ngưỡng peer     | ✖                               | Giới hạn số peer cùng time slot.         |
| `SEEDER_CACHE_PATH`                            | ✖                         | ✅ (ví dụ `/var/cinemate/cache`) | Thư mục mount chứa segment `.ts`.        |
| `SEEDER_ENABLED`                               | ✖                         | Mặc định `true`                 | Tắt nếu chỉ muốn quan sát cache.         |

## 4. Docker Compose mẫu
```yaml
version: "3.9"
services:
  streaming-signaling:
    image: cinemate/streaming-signaling:latest
    environment:
      SERVER_PORT: 8083
      REDIS_HOST: redis
      REDIS_PORT: 6379
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      STREAMING_TOPIC_PREFIX: stream.
      KAFKA_CONSUMER_GROUP: signaling-cluster
    ports:
      - "8083:8083"
    depends_on:
      - redis
      - kafka

  streaming-seeder:
    image: cinemate/streaming-seeder:latest
    environment:
      SERVER_PORT: 8084
      REDIS_HOST: redis
      REDIS_PORT: 6379
      SEEDER_CACHE_PATH: /var/cinemate/cache
    volumes:
      - /data/cinemate/cache:/var/cinemate/cache:rw
    depends_on:
      - redis

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis-data:/data

  kafka:
    image: bitnami/kafka:3.7
    environment:
      KAFKA_ENABLE_KRAFT: "yes"
      KAFKA_CFG_NODE_ID: "0"
      KAFKA_CFG_PROCESS_ROLES: "broker,controller"
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_CFG_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093"
      KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP: "PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT"
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: "0@kafka:9093"
      KAFKA_CFG_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092"
    volumes:
      - kafka-data:/bitnami

volumes:
  redis-data:
  kafka-data:
```
- `streaming-signaling` expose cổng 8083 cho client WebSocket.
- `streaming-seeder` không cần expose port public; chỉ cần mount thư mục cache để phục vụ các peer trong mạng nội bộ.
- Có thể bổ sung Prometheus/Grafana hoặc công cụ quan sát khác nếu muốn theo dõi chuyên sâu.

## 5. Kubernetes (gợi ý)
1. Tạo hai `Deployment` riêng; `streaming-signaling` có thể replica >1 để HA, `streaming-seeder` scale theo số cache node.
2. Lưu cấu hình chung (`application.yml`, biến Redis, Kafka) trong `ConfigMap`/`Secret`; chỉ signaling cần Kafka credentials.
3. Thêm probe:
   - Signaling: `HTTP GET /actuator/health/readiness` và `.../liveness` trên port 8083.
   - Seeder: `HTTP GET /actuator/health` hoặc `exec` kiểm tra cache path trên port 8084.
4. Cấp `PersistentVolumeClaim` cho seeder để bảo toàn cache giữa các pod restart.
5. Dùng `podAntiAffinity`/`topologySpreadConstraints` cho signaling để tránh cùng node; seeder ưu tiên node có dung lượng đĩa lớn.
6. Áp dụng `HorizontalPodAutoscaler` cho signaling dựa trên CPU hoặc metrics WebSocket concurrent sessions.

## 6. Checklist kiểm tra sau deploy
- Redis liên tục xuất hiện key `stream:<id>:segment:*` và `stream:<id>:peers`, TTL được gia hạn đúng kỳ vọng.
- WebSocket `/ws/signaling` trả về `PeerListMessage` ngay khi client đăng nhập và phản hồi ping đúng tần suất.
- Seeder log `Seeder ready to serve peers` ở startup và log maintenance định kỳ không báo lỗi cache.
- Kafka topic `stream.<id>.events` nhận message, signaling log `Subscribed to Kafka topic ...` và không báo lỗi consumer.
- Redis metric `successRate` của peer tăng dần, chứng tỏ seeder phân phối segment hiệu quả.

## 7. Các bước mở rộng tiếp theo
- Bổ sung metrics Prometheus riêng cho signaling (WebSocket sessions, Kafka lag) và seeder (cache scan duration) để so sánh hiệu năng.
- Triển khai canary cho seeder khi thay đổi chiến lược cache nhằm giảm rủi ro ảnh hưởng viewer.
- Đặt cảnh báo khi Redis không thấy update `lastSeen` trong thời gian cấu hình để phát hiện seeder ngừng hoạt động.
