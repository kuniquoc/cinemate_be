# Streaming Seeder Service

The streaming-seeder service keeps Redis in sync with locally cached media segments so that peers can discover which nodes can seed content.

## Responsibilities

- Scan the configured cache directory for stream segments.
- Register available segments in Redis with TTL refresh.
- Periodically prune expired cache files and clean up Redis metadata.
- Expose actuator health endpoints for operational monitoring.

## Configuration

Environment variables (defaults in `src/main/resources/application.yml`):

| Variable                                       | Description                                       |
| ---------------------------------------------- | ------------------------------------------------- |
| `SERVER_PORT`                                  | HTTP port (default `8084`).                       |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection parameters.                      |
| `SEEDER_ENABLED`                               | Toggle startup sync and scheduled maintenance.    |
| `SEEDER_CACHE_PATH`                            | Directory containing media segments.              |
| `JAVA_OPTS`                                    | Additional JVM options for the runtime container. |

## Running Locally

```bash
mvn -pl streaming-seeder -am spring-boot:run
```

Ensure the cache directory exists and Redis is reachable before starting the service.

## Testing

```bash
mvn -pl streaming-seeder test
```
