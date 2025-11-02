# Streaming Deployment Guide (Signaling & Seeder)

## 1. Kiến trúc tách hai container
- **Signaling Server**: phục vụ WebSocket `/ws/signaling`, lắng nghe Kafka và cập nhật Redis cho các peer đang hoạt động. Không cần truy cập filesystem cache.
- **Seeder Worker**: chịu trách nhiệm quét cache, đồng bộ metadata segment vào Redis và dọn dẹp file hết hạn. Có thể tắt WebSocket và chỉ chạy các scheduler.
- Cùng chia sẻ Redis (lưu peer/segment state) và Kafka (phát sự kiện stream). Tách container cho phép scale độc lập: signaling scale theo số viewer, seeder scale theo số node lưu cache.

## 2. Chuẩn bị build image
1. Biên dịch module streaming:
   ```powershell
   mvn -pl streaming -am clean package
   ```
2. Build image cho signaling:
   ```powershell
   docker build -t cinemate/streaming-signaling:latest -f streaming/Dockerfile streaming
   ```
3. Build image cho seeder (truyền profile phù hợp):
   ```powershell
   docker build --build-arg ACTIVE_PROFILES=seeder -t cinemate/streaming-seeder:latest -f streaming/Dockerfile streaming
   ```
4. Nếu Dockerfile chưa có `ARG ACTIVE_PROFILES`, thêm:
   ```dockerfile
   ARG ACTIVE_PROFILES=prod
   ENV SPRING_PROFILES_ACTIVE=${ACTIVE_PROFILES}
   ```
   Khi chạy local có thể đặt `ACTIVE_PROFILES=dev`, khi build seeder đặt `seeder`.

## 3. Biến môi trường cốt lõi
| Tên | Sử dụng |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` cho signaling, `seeder` cho worker. |
| `SEEDER_ENABLED` | `false` ở signaling, `true` ở seeder. |
| `SEEDER_CACHE_PATH` | Chỉ cần cho seeder, trỏ tới thư mục mount (ví dụ `/var/cinemate/cache`). |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Thông tin kết nối Redis dùng chung. |
| `KAFKA_BOOTSTRAP_SERVERS` | Broker Kafka cho cả hai container. |
| `KAFKA_CONSUMER_GROUP` | Có thể đặt khác nhau (ví dụ `signaling-cluster` và `seeder-workers`) để tách offset. |
| `STREAMING_TOPIC_PREFIX` | Tiền tố topic sự kiện stream. |

## 4. Docker Compose mẫu
```yaml
version: "3.9"
services:
  streaming-signaling:
    image: cinemate/streaming-signaling:latest
    environment:
      SPRING_PROFILES_ACTIVE: "prod"
      REDIS_HOST: redis
      REDIS_PORT: 6379
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      STREAMING_TOPIC_PREFIX: stream.
      KAFKA_CONSUMER_GROUP: signaling-cluster
      SEEDER_ENABLED: "false"
    ports:
      - "8083:8083"
    depends_on:
      - redis
      - kafka

  streaming-seeder:
    image: cinemate/streaming-seeder:latest
    environment:
      SPRING_PROFILES_ACTIVE: "seeder"
      REDIS_HOST: redis
      REDIS_PORT: 6379
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      STREAMING_TOPIC_PREFIX: stream.
      KAFKA_CONSUMER_GROUP: seeder-workers
      SEEDER_ENABLED: "true"
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
- `streaming-signaling` công bố cổng 8083 để client kết nối WebSocket.
- `streaming-seeder` không cần expose cổng; mount thư mục cache để lưu `.ts`.
- Có thể mở rộng thêm service Prometheus/Grafana nếu cần theo dõi.

## 5. Kubernetes (gợi ý)
1. Tạo hai `Deployment` riêng; `streaming-signaling` có thể replica >1 để HA, `streaming-seeder` tùy nhu cầu cache.
2. Dùng `ConfigMap` lưu `application.yml` chung; `Secret` chứa Redis/Kafka credentials.
3. Thêm `readinessProbe`/`livenessProbe`:
   - Signaling: `HTTP GET /actuator/health/readiness` và `.../liveness`.
   - Seeder: có thể dùng `exec` kiểm tra tồn tại thư mục cache hoặc `HTTP GET /actuator/health` nếu giữ actuator.
4. `PersistentVolumeClaim` cho seeder để bảo toàn cache.
5. Áp dụng `podAntiAffinity` để signaling pods nằm trên node khác nhau; seeder có thể pin vào các node có dung lượng đĩa lớn.
6. Sử dụng `HorizontalPodAutoscaler` cho signaling dựa trên CPU/metrics WebSocket. Seeder thường cố định nhưng có thể scale theo số stream đang seed.

## 6. Checklist kiểm tra sau deploy
- [ ] Redis liên tục xuất hiện key `stream:<id>:segment:*` và `stream:<id>:peers` (kiểm tra TTL đang gia hạn).
- [ ] WebSocket `/ws/signaling` trả về `PeerListMessage` ngay khi client đăng nhập.
- [ ] Seeder log `Seeder ready to serve peers` lúc khởi động và log maintenance định kỳ.
- [ ] Kafka topic `stream.<id>.events` nhận message và signaling ghi log debug.
- [ ] Tỷ lệ cache hit các peer tăng (trường `successRate` trong Redis), chứng tỏ seeder hoạt động.

## 7. Các bước mở rộng tiếp theo
- Thêm metrics Prometheus riêng cho signaling và seeder để dễ so sánh hiệu năng.
- Áp dụng Canary release: triển khai seeder phiên bản mới song song để đánh giá trước khi scale rộng.
- Đặt alert khi Redis không thấy update `lastActive` trong thời gian cấu hình, nhằm phát hiện seeder ngừng hoạt động.
