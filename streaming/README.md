# Streaming Service

The Streaming service coordinates P2P segment distribution for the Cinemate platform. It exposes a WebSocket-based signaling layer and maintains Seeder caches via Redis to keep segment availability fresh.

## Features

- WebSocket signaling endpoint `/ws/signaling` for peer coordination (`WHO_HAS`, `REPORT_SEGMENT`).
- Redis-backed coverage maps of segment ownership with automatic TTL refresh.
- Kafka-based event subscription per stream for cross-node signaling fan-out.
- Seeder startup routine that syncs the local cache window into Redis.
- Scheduled cache maintenance to prune expired segments and refresh TTLs.
- Actuator endpoints for health checks.

## Configuration

Environment variables (defaults shown in `application.yml`):

| Variable | Description |
| --- | --- |
| `SERVER_PORT` | HTTP port (default `8083`). |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection parameters. |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers list (default `localhost:9092`). |
| `KAFKA_CONSUMER_GROUP` | Kafka consumer group for signaling subscriptions. |
| `SEEDER_ENABLED` | Enable Seeder startup & maintenance (`true` in prod, `false` in dev). |
| `SEEDER_CACHE_PATH` | Absolute/relative path to the Seeder cache directory. |
| `STREAMING_MAX_ACTIVE_PEERS` | Max concurrent peer connections per viewer. |
| `JAVA_OPTS` | Additional JVM flags inside the container. |

The `streaming.*` namespace in `application.yml` exposes further tuning knobs for TTLs, maintenance interval, and playback heuristics.

## Running Locally

```bash
mvn -pl streaming -am spring-boot:run
```

Redis and Kafka must be reachable according to the environment variables. For development, enable the Seeder loop by exporting `SEEDER_ENABLED=true` and point `SEEDER_CACHE_PATH` to a directory containing cache files structured as `<cache>/<streamId>/<segmentId>.ts`.

## Testing

```bash
mvn -pl streaming test
```

Unit tests cover the seeder cache window logic and signaling Redis mappings. Add integration tests if you introduce new adapters (e.g., Redis streams, WebRTC).
