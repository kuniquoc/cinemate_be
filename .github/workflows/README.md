# CI/CD Setup Guide - Cinemate Backend

## 📋 Tổng quan

CI/CD pipeline của Cinemate Backend bao gồm:
- **ci-cd.yml**: Workflow tự động chạy khi push code vào `main` hoặc `master`
- **manual-deploy.yml**: Workflow chạy thủ công để deploy hoặc rebuild toàn bộ

## 🔐 Cấu hình GitHub Secrets

Vào **Settings > Secrets and variables > Actions** của repository và thêm các secrets sau:

### Docker Hub Credentials
| Secret Name       | Mô tả                                 | Ví dụ          |
| ----------------- | ------------------------------------- | -------------- |
| `DOCKER_USERNAME` | Docker Hub username                   | `kuniquoc24`   |
| `DOCKER_PASSWORD` | Docker Hub password hoặc Access Token | `dckr_pat_xxx` |

### Server SSH Credentials
| Secret Name       | Mô tả                            | Ví dụ           |
| ----------------- | -------------------------------- | --------------- |
| `SERVER_HOST`     | IP hoặc hostname của server      | `192.168.1.100` |
| `SERVER_USERNAME` | SSH username                     | `ubuntu`        |
| `SERVER_PASSWORD` | SSH password                     | `your_password` |
| `SERVER_SSH_PORT` | SSH port (optional, default: 22) | `22`            |
| `DEPLOY_PATH`     | Đường dẫn deploy trên server     | `/opt/cinemate` |

### Database Credentials
| Secret Name                     | Mô tả                             |
| ------------------------------- | --------------------------------- |
| `AUTH_POSTGRES_USER`            | Username cho auth-postgres        |
| `AUTH_POSTGRES_PASSWORD`        | Password cho auth-postgres        |
| `MOVIE_POSTGRES_USER`           | Username cho movie-postgres       |
| `MOVIE_POSTGRES_PASSWORD`       | Password cho movie-postgres       |
| `CUSTOMER_POSTGRES_USER`        | Username cho customer-postgres    |
| `CUSTOMER_POSTGRES_PASSWORD`    | Password cho customer-postgres    |
| `INTERACTION_POSTGRES_USER`     | Username cho interaction-postgres |
| `INTERACTION_POSTGRES_PASSWORD` | Password cho interaction-postgres |
| `PAYMENT_POSTGRES_USER`         | Username cho payment-postgres     |
| `PAYMENT_POSTGRES_PASSWORD`     | Password cho payment-postgres     |

### MinIO Credentials
| Secret Name        | Mô tả            |
| ------------------ | ---------------- |
| `MINIO_ACCESS_KEY` | MinIO access key |
| `MINIO_SECRET_KEY` | MinIO secret key |

### Environment Files (Nội dung đầy đủ của từng file .env)
| Secret Name                   | Mô tả                                           |
| ----------------------------- | ----------------------------------------------- |
| `ENV_AUTH_SERVICE`            | Nội dung file `env/auth-service.env`            |
| `ENV_CUSTOMER_SERVICE`        | Nội dung file `env/customer-service.env`        |
| `ENV_GATEWAY`                 | Nội dung file `env/gateway.env`                 |
| `ENV_INTERACTION_RECOMMENDER` | Nội dung file `env/interaction-recommender.env` |
| `ENV_MOVIE_SERVICE`           | Nội dung file `env/movie-service.env`           |
| `ENV_PAYMENT_SERVICE`         | Nội dung file `env/payment-service.env`         |
| `ENV_STREAMING_SEEDER`        | Nội dung file `env/streaming-seeder.env`        |
| `ENV_STREAMING_SIGNALING`     | Nội dung file `env/streaming-signaling.env`     |

### Check kĩ file ci-cd.yml

## 🖥️ Chuẩn bị Server

### 1. Cài đặt Docker
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Cài Docker Compose plugin
sudo apt-get update
sudo apt-get install docker-compose-plugin
```

### 2. Tạo thư mục deploy
```bash
sudo mkdir -p /opt/cinemate
sudo chown $USER:$USER /opt/cinemate
```

### 3. Cấu hình SSH password authentication (nếu cần)
```bash
# Đảm bảo server cho phép password authentication
sudo nano /etc/ssh/sshd_config

# Kiểm tra các dòng sau:
# PasswordAuthentication yes

# Restart SSH service
sudo systemctl restart sshd
```

**Lưu ý bảo mật**: Nên sử dụng mật khẩu mạnh và cân nhắc giới hạn IP được phép SSH.

## 🚀 Cách sử dụng

### Automatic Deployment
Mỗi khi push code vào branch `main` hoặc `master`:
1. CI/CD sẽ detect services nào thay đổi
2. Build và push Docker images của các services đó
3. SSH vào server và deploy

### Manual Deployment
1. Vào tab **Actions** trên GitHub
2. Chọn workflow **Manual Deploy**
3. Click **Run workflow**
4. Chọn options:
   - **Environment**: `production` hoặc `staging`
   - **Services**: `all` hoặc danh sách services cách nhau bằng dấu phẩy
   - **Force rebuild**: Tick nếu muốn build lại tất cả images

## 🔄 Đảm bảo Image Mới Nhất

Pipeline đã được cấu hình để đảm bảo server luôn sử dụng image mới nhất:

1. **Multi-tag strategy**: Mỗi image được tag với:
   - `latest`
   - Git SHA (ví dụ: `abc1234`)
   - Timestamp (ví dụ: `20231207-143052`)

2. **Force pull**: Sử dụng `docker compose pull` trước khi start

3. **Force recreate**: Sử dụng `--force-recreate --pull always` khi start containers

4. **Prune images**: Xóa dangling images sau mỗi lần deploy

## 📁 Cấu trúc thư mục trên Server

```
/opt/cinemate/
├── docker-compose.yml
├── .env                           # Database & MinIO credentials
├── env/
│   ├── auth-service.env
│   ├── customer-service.env
│   ├── gateway.env
│   ├── interaction-recommender.env
│   ├── movie-service.env
│   ├── payment-service.env
│   ├── streaming-seeder.env
│   └── streaming-signaling.env
└── src/
    └── interaction-recommender-service/
        └── scripts/
            └── init_db.sql
```

## 🔧 Troubleshooting

### Image không được cập nhật
```bash
# Trên server, force pull và recreate
cd /opt/cinemate
docker compose pull
docker compose up -d --force-recreate --pull always
```

### Kiểm tra logs
```bash
# Xem logs của tất cả services
docker compose logs -f

# Xem logs của service cụ thể
docker compose logs -f auth-service
```

### Kiểm tra trạng thái
```bash
docker compose ps
docker compose top
```

### Restart service cụ thể
```bash
docker compose restart auth-service
```

### Xóa và chạy lại từ đầu
```bash
docker compose down -v  # Cẩn thận: sẽ xóa volumes!
docker compose up -d --force-recreate --pull always
```

## 📊 Monitoring

Sau khi deploy, có thể kiểm tra health của các services:

| Service                 | Health Endpoint                       |
| ----------------------- | ------------------------------------- |
| Gateway                 | http://localhost:8080/actuator/health |
| Auth Service            | http://localhost:8085/actuator/health |
| Movie Service           | http://localhost:8081/actuator/health |
| Customer Service        | http://localhost:8082/actuator/health |
| Streaming Signaling     | http://localhost:8083/actuator/health |
| Streaming Seeder        | http://localhost:8084/actuator/health |
| Interaction Recommender | http://localhost:8088/health          |
| MinIO Console           | http://localhost:9001                 |
| Kafka UI                | http://localhost:8087                 |
