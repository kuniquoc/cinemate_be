# Interaction Recommender Service

A FastAPI-based service for tracking user interactions and providing movie recommendations for the Cinemate Platform.

## Features

- **Event Tracking**: Track user interactions (watch, search, rating, favorite)
- **Feature Extraction**: Real-time feature extraction from user interactions
- **Recommendations**: ML-based personalized movie recommendations
- **Feedback Collection**: Collect user feedback for model improvement
- **Kafka Integration**: Event streaming for scalability and replay capability

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                 interaction-recommender-service                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  HTTP API   │  │   Kafka     │  │   Recommendation        │  │
│  │  (FastAPI)  │  │  Consumer   │  │   Engine                │  │
│  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │
│         │                │                      │                │
│         v                v                      v                │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    Feature Extractor                        ││
│  └─────────────────────────────────────────────────────────────┘│
│         │                │                      │                │
│         v                v                      v                │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │  PostgreSQL  │  │    Redis     │  │      Kafka            │  │
│  │  (events)    │  │   (cache)    │  │    (streaming)        │  │
│  └──────────────┘  └──────────────┘  └───────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Events

- `POST /api/v1/events/watch` - Track watch event
- `POST /api/v1/events/search` - Track search event
- `POST /api/v1/events/rating` - Track rating event
- `POST /api/v1/events/favorite` - Track favorite event

### Recommendations

- `GET /api/v1/recommend/{user_id}` - Get personalized recommendations
- `GET /api/v1/features/{user_id}` - Get user features
- `POST /api/v1/features/{user_id}/refresh` - Refresh user features

### Feedback

- `POST /api/v1/feedback` - Submit recommendation feedback

### System

- `GET /api/v1/health` - Full health check
- `GET /api/v1/health/live` - Liveness probe
- `GET /api/v1/health/ready` - Readiness probe
- `GET /model/info` - Model information
- `POST /model/reload` - Reload model

## Quick Start

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Redis 7+
- Kafka (optional)

### Local Development

1. Create virtual environment:

```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
.\venv\Scripts\activate   # Windows
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Set environment variables:

```bash
cp .env.example .env
# Edit .env with your settings
```

4. Run database migrations:

```bash
alembic upgrade head
```

5. Start the service:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Docker

```bash
docker build -t interaction-recommender-service .
docker run -p 8090:8000 interaction-recommender-service
```

## Configuration

| Variable                  | Description                  | Default                                                               |
| ------------------------- | ---------------------------- | --------------------------------------------------------------------- |
| `DATABASE_URL`            | PostgreSQL connection string | `postgresql+asyncpg://admin:admin@interaction-db:5432/interaction_db` |
| `REDIS_URL`               | Redis connection string      | `redis://cinemate-redis:6379`                                         |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers                | `cinemate-broker:9092`                                                |
| `MODEL_PATH`              | Path to ML model file        | `/app/models/recommender.pkl`                                         |
| `ENABLE_CONSUMERS`        | Enable Kafka consumers       | `true`                                                                |
| `ENABLE_KAFKA`            | Enable Kafka integration     | `true`                                                                |

## Kafka Topics

| Topic                    | Producer     | Consumer                    | Purpose           |
| ------------------------ | ------------ | --------------------------- | ----------------- |
| `interaction_events`     | This service | This service + offline jobs | Raw events        |
| `processed_features`     | This service | Offline training            | Computed features |
| `model_feedback`         | This service | Offline training            | User feedback     |
| `interaction_events_dlq` | This service | Manual review               | Dead letter queue |

## Model

The recommendation model is loaded from `MODEL_PATH` at startup. Supported formats:

- `.pkl` - Scikit-learn models (joblib)
- `.onnx` - ONNX models (planned)

To reload the model without restart:

```bash
curl -X POST http://localhost:8090/model/reload
```

## Database Schema

See `scripts/init_db.sql` for the complete schema.

Main tables:

- `interaction_events` - Raw user interactions
- `user_features` - Computed user features
- `audit_events` - Processing audit trail
- `model_feedback` - Recommendation feedback

## Testing

```bash
pytest tests/ -v --cov=app
```

## License

Proprietary - Cinemate Platform
