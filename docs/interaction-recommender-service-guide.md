# Interaction Recommender Service - Integration Guide

## Overview

The `interaction-recommender-service` is a Python/FastAPI service that handles user interactions and provides movie recommendations for the Cinemate Platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Cinemate Platform                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐      ┌─────────────────┐      ┌─────────────────────────┐  │
│  │   Gateway   │──────│  movie-service  │──────│ interaction-recommender │  │
│  │   (8080)    │      │     (8081)      │      │       (8090)            │  │
│  └─────────────┘      └─────────────────┘      └─────────────────────────┘  │
│                              │                           │                   │
│                              │                           │                   │
│                    ┌─────────▼───────────────────────────▼─────────┐        │
│                    │                  Kafka                         │        │
│                    │         (interaction_events topic)             │        │
│                    └───────────────────────────────────────────────┘        │
│                                                                              │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────────────────┐      │
│  │  movie-db   │      │   Redis     │      │   interaction-db        │      │
│  │  (5432)     │      │   (6379)    │      │   (5433)                │      │
│  └─────────────┘      └─────────────┘      └─────────────────────────┘      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Event Tracking

| Endpoint                  | Method | Description          |
| ------------------------- | ------ | -------------------- |
| `/api/v1/events/watch`    | POST   | Track watch event    |
| `/api/v1/events/search`   | POST   | Track search event   |
| `/api/v1/events/rating`   | POST   | Track rating event   |
| `/api/v1/events/favorite` | POST   | Track favorite event |

### Recommendations

| Endpoint                             | Method | Description                      |
| ------------------------------------ | ------ | -------------------------------- |
| `/api/v1/recommend/{user_id}`        | GET    | Get personalized recommendations |
| `/api/v1/features/{user_id}`         | GET    | Get user features                |
| `/api/v1/features/{user_id}/refresh` | POST   | Refresh user features            |

### Feedback

| Endpoint           | Method | Description                    |
| ------------------ | ------ | ------------------------------ |
| `/api/v1/feedback` | POST   | Submit recommendation feedback |

### System

| Endpoint               | Method | Description       |
| ---------------------- | ------ | ----------------- |
| `/api/v1/health`       | GET    | Full health check |
| `/api/v1/health/live`  | GET    | Liveness probe    |
| `/api/v1/health/ready` | GET    | Readiness probe   |
| `/api/v1/model/info`   | GET    | Model information |
| `/api/v1/model/reload` | POST   | Reload model      |

## Integration with movie-service

### 1. Add Feign Client Dependency

The dependency is already added to `movie-service/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 2. Enable Feign Clients

Add `@EnableFeignClients` to `MovieApplication.java` (already done):

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.pbl6.cinemate.movie.client")
public class MovieApplication { ... }
```

### 3. Use InteractionService

Inject `InteractionService` to track events:

```java
@Service
@RequiredArgsConstructor
public class MovieWatchService {
    
    private final InteractionService interactionService;
    
    public void onMovieWatched(UUID userId, UUID movieId, int duration) {
        // Fire-and-forget tracking
        interactionService.trackWatch(userId, movieId, duration, "web");
    }
    
    public List<Movie> getRecommendedMovies(UUID userId) {
        // Get recommendations
        RecommendationResponse response = interactionService.getHomeRecommendations(userId, 20);
        List<UUID> movieIds = response.getMovieIds();
        return movieRepository.findAllById(movieIds);
    }
}
```

### 4. Configuration

Add to `application.yml`:

```yaml
interaction:
  recommender:
    service:
      url: ${INTERACTION_RECOMMENDER_URL:http://interaction-recommender-service:8000}
      enabled: ${INTERACTION_RECOMMENDER_ENABLED:true}
```

## Kafka Topics

| Topic                    | Producer                        | Consumer                        | Purpose                   |
| ------------------------ | ------------------------------- | ------------------------------- | ------------------------- |
| `interaction_events`     | interaction-recommender-service | interaction-recommender-service | Raw events for processing |
| `processed_features`     | interaction-recommender-service | Offline training jobs           | Computed features         |
| `model_feedback`         | interaction-recommender-service | Offline training jobs           | User feedback             |
| `interaction_events_dlq` | interaction-recommender-service | Manual review                   | Dead letter queue         |

## Data Flow

1. **Event Tracking**:
   ```
   Frontend → Gateway → movie-service → POST /events/watch → interaction-recommender-service
                                                                    │
                                                                    ▼
                                                         ┌─────────────────┐
                                                         │  PostgreSQL     │
                                                         │  (raw events)   │
                                                         └────────┬────────┘
                                                                  │
                                                                  ▼
                                                         ┌─────────────────┐
                                                         │     Kafka       │
                                                         │ (interaction_   │
                                                         │     events)     │
                                                         └────────┬────────┘
                                                                  │
                                                                  ▼
                                                         ┌─────────────────┐
                                                         │ Feature Extract │
                                                         └────────┬────────┘
                                                                  │
                                                         ┌────────▼────────┐
                                                         │     Redis       │
                                                         │ (user_features) │
                                                         └─────────────────┘
   ```

2. **Recommendations**:
   ```
   movie-service → GET /recommend/{user_id} → interaction-recommender-service
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │   Load Features     │
                                              │   (Redis/Postgres)  │
                                              └──────────┬──────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │   ML Model          │
                                              │   Inference         │
                                              └──────────┬──────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │   Return            │
                                              │   Recommendations   │
                                              └─────────────────────┘
   ```

## Docker Deployment

### Start Services

```bash
docker-compose up -d interaction-db interaction-recommender-service
```

### Environment Variables

| Variable                  | Description              | Default                                                               |
| ------------------------- | ------------------------ | --------------------------------------------------------------------- |
| `DATABASE_URL`            | PostgreSQL connection    | `postgresql+asyncpg://admin:admin@interaction-db:5432/interaction_db` |
| `REDIS_URL`               | Redis connection         | `redis://cinemate-redis:6379`                                         |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers            | `cinemate-broker:9092`                                                |
| `MODEL_PATH`              | ML model path            | `/app/models/recommender.pkl`                                         |
| `ENABLE_CONSUMERS`        | Enable Kafka consumers   | `true`                                                                |
| `ENABLE_KAFKA`            | Enable Kafka integration | `true`                                                                |

### Ports

| Service                         | Internal Port | External Port |
| ------------------------------- | ------------- | ------------- |
| interaction-recommender-service | 8000          | 8090          |
| interaction-db                  | 5432          | 5433          |

## Model Management

### Model Format

The service supports `.pkl` (joblib) models. The model should implement:

- `predict_proba(X)` for classification
- `predict(X)` for regression

### Hot Reload

Reload model without restart:

```bash
curl -X POST http://localhost:8090/model/reload
```

### Create Sample Model

```bash
cd src/interaction-recommender-service
python scripts/create_sample_model.py
```

## Monitoring

### Health Checks

```bash
# Full health check
curl http://localhost:8090/health

# Kubernetes liveness
curl http://localhost:8090/health/live

# Kubernetes readiness
curl http://localhost:8090/health/ready
```

### Logs

```bash
docker logs cinemate-interaction-recommender -f
```

## Testing

### API Testing

```bash
# Track watch event
curl -X POST http://localhost:8090/events/watch \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "movieId": "550e8400-e29b-41d4-a716-446655440001",
    "clientTimestamp": "2025-01-01T12:00:00Z",
    "metadata": {
      "watchDuration": 3600,
      "device": "web",
      "quality": "1080p"
    }
  }'

# Get recommendations
curl "http://localhost:8090/recommend/550e8400-e29b-41d4-a716-446655440000?k=10&context=home"
```

### Python Tests

```bash
cd src/interaction-recommender-service
pip install -r requirements.txt
pytest tests/ -v
```

## Troubleshooting

### Service Not Starting

1. Check database connection:
   ```bash
   docker logs cinemate-interaction-postgres
   ```

2. Check Redis connection:
   ```bash
   docker exec cinemate-redis redis-cli ping
   ```

3. Check Kafka connection:
   ```bash
   docker logs cinemate-broker
   ```

### No Recommendations

1. Check if user has features:
   ```bash
   curl http://localhost:8090/features/{user_id}
   ```

2. Check model status:
   ```bash
   curl http://localhost:8090/model/info
   ```

3. Check service health:
   ```bash
   curl http://localhost:8090/health
   ```

### Kafka Consumer Not Processing

1. Check consumer status in health endpoint
2. Check DLQ for failed messages:
   ```bash
   # Using kafka-ui at http://localhost:8989
   ```

3. Check logs for errors:
   ```bash
   docker logs cinemate-interaction-recommender | grep -i error
   ```
