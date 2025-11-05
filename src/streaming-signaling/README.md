# Streaming Signaling Service

The streaming-signaling service coordinates peer discovery for movie playback via a WebSocket endpoint and Redis/Kafka backplane.

## Features

- WebSocket endpoint `/ws/signaling` supporting `WHO_HAS` and `REPORT_SEGMENT` messages.
- Redis-backed peer, segment, and metrics registries with TTL refresh.
- Kafka subscriber per stream to fan-out playback events.
- Actuator endpoints for health and metrics.

## Configuration

Environment variables (defaults in `src/main/resources/application.yml`):

| Variable                                       | Description                                    |
| ---------------------------------------------- | ---------------------------------------------- |
| `SERVER_PORT`                                  | HTTP port (default `8083`).                    |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection.                              |
| `KAFKA_BOOTSTRAP_SERVERS`                      | Kafka brokers list (default `localhost:9092`). |
| `KAFKA_CONSUMER_GROUP`                         | Kafka consumer group id.                       |
| `STREAMING_TOPIC_PREFIX`                       | Prefix for per-stream Kafka topics.            |
| `STREAMING_MAX_ACTIVE_PEERS`                   | Max concurrent peer connections per viewer.    |

## Local Development

```bash
mvn -pl streaming-signaling -am spring-boot:run
```

Redis and Kafka must be reachable according to the configured environment variables.

## Testing

```bash
mvn -pl streaming-signaling test
```
