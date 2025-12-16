# Streaming Seeder Service

The streaming-seeder service keeps Redis in sync with locally cached media segments so that peers can discover which
nodes can seed content.

## Responsibilities

- Scan the configured cache directory for stream segments.
- Fetch missing segments from the MinIO origin when requested and persist them locally.
- Register available segments in Redis with TTL refresh.
- Periodically prune expired cache files and clean up Redis metadata.
- Expose actuator health endpoints for operational monitoring.
- Serve cached segments over HTTP for viewer fallback (`GET /streams/{streamId}/segments/{segmentId}`).

**Note:** `segmentId` now includes the file extension (e.g., `seg_0005.m4s`), making it equivalent to the filename.

## Configuration

Environment variables (defaults in `src/main/resources/application.yml`):

| Variable                                       | Description                                               |
|------------------------------------------------|-----------------------------------------------------------|
| `SERVER_PORT`                                  | HTTP port (default `8084`).                               |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection parameters.                              |
| `SEEDER_ENABLED`                               | Toggle startup sync and scheduled maintenance.            |
| `SEEDER_CACHE_PATH`                            | Directory containing media segments.                      |
| `SEEDER_ORIGIN_ENABLED`                        | Enable MinIO pull-through caching (default `true`).       |
| `SEEDER_ORIGIN_PREFIX`                         | Object prefix inside the MinIO bucket (default `movies`). |
| `MINIO_ENDPOINT`                               | MinIO endpoint the seeder pulls segments from.            |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`        | MinIO credentials with read permission.                   |
| `MINIO_BUCKET`                                 | Bucket containing transcoded HLS assets.                  |
| `JAVA_OPTS`                                    | Additional JVM options for the runtime container.         |

## Running Locally

```bash
mvn -pl streaming-seeder -am spring-boot:run
```

Ensure the cache directory exists and Redis is reachable before starting the service.

## Testing

```bash
mvn -pl streaming-seeder test
```
