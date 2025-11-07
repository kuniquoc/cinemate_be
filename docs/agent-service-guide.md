# Agent Service Playbook (based on `movie-service`)

This guide captures the rules, structure, and checklist for spinning up a new microservice in this mono-repo using `movie-service` as the canonical reference. The goal is to help agents (or developers) deliver a service that stays consistent in source, configuration, Docker packaging, security, and operations.

## Contract At A Glance

- Inputs: Java (Spring Boot) code, resource files (`application.yml*`), Dockerfile, module `pom.xml`
- Outputs: a Maven module in the mono-repo, Docker image with healthcheck, documented environment configuration, baseline tests
- Error modes: validation failure ⇒ return 4xx; server errors ⇒ 5xx; missing config ⇒ fail fast at startup
- Success criteria: Maven multi-module build succeeds, container starts, `/actuator/health` reports `UP`

## Core Principles

- Java 21 (same as the `movie-service` module `pom.xml`).
- Package namespace: `com.pbl6.cinemate.<service-name>`.
- Register the module under the parent multi-module `pom` with `artifactId` = service name.
- Minimum dependencies: `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `spring-boot-starter-web`.
- Reuse shared components via the `shared-kernel` module when you need common value objects, exceptions, or utilities.
- Use Lombok and MapStruct where appropriate; wire annotation processors in `pom.xml` following the parent pattern.
- DTOs: favor Java `record` types for straightforward request/response payloads (similar to `MovieUploadRequest`). Add validation annotations to record components when needed.
- Write unit tests covering service layer and controllers (happy path plus at least one validation/error scenario).

## Suggested Module Layout

- `src/main/java/com/pbl6/cinemate/<service>/...` (controllers, services, dto, config, exceptions)
- `src/main/resources/application.yml` (defaults), with profile overrides such as `application-dev.yml`, `application-prod.yml` if required
- `src/main/resources/db/migration` (Flyway scripts) when the service owns database schema
- `Dockerfile` at the module root
- Module-level `pom.xml` declaring dependencies and plugins

## Source & Coding Conventions

- Controllers stay thin: map requests, call service layer, return DTOs. Avoid embedding business logic.
- Services can expose an interface plus implementation when multiple implementations are likely; interfaces simplify testing.
- Organize DTOs by responsibility (e.g., `request` vs `response` packages). Use `record` types when they reduce boilerplate without sacrificing clarity.
- Validation: leverage `jakarta.validation` (Hibernate Validator) with `@Valid` on controller parameters.
  - Example: `public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request)`
- Mapping: use MapStruct for Entity ↔ DTO transformations.
- Exception handling: implement a global `@ControllerAdvice` to standardize HTTP status and payload (`Problem`/`ApiError`).
- Logging: rely on `slf4j` via Lombok `@Slf4j`; manage log levels in `application.yml`.

Edge cases to guard against:
- Empty or malformed request bodies ⇒ respond with 400
- Validation violations ⇒ return structured error list (field + message)
- Large multipart uploads ⇒ tune multipart limits and stream processing
- External dependencies (DB/MinIO) down ⇒ fail fast at startup or degrade gracefully (e.g., circuit breaker)

## Validation & Error Handling

- Use `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern`, etc., on DTO components.
- Error responses should stick to a common envelope: `{timestamp, status, error, message, path, errors?}`
- Global handler should capture `MethodArgumentNotValidException` (and similar) to surface field-level errors predictably.

## Dockerfile Guidance

The repo relies on multi-stage builds mirroring `movie-service`.

- Build stage: `maven:3.9.9-eclipse-temurin-21`
  - Run `mvn -f pom.xml -pl <module> -am -B -DskipTests package` (or use `dependency:go-offline`) to warm dependency cache.
- Runtime stage: `eclipse-temurin:21-jre-alpine`
  - Install required utilities (e.g., `ffmpeg`, `curl`) as needed.
  - Create a non-root user (e.g., `spring`) and `chown` the jar.
  - Expose port 8080 and wire a `HEALTHCHECK` to `/actuator/health`.
  - Support `JAVA_OPTS` through the entrypoint.

Sample Dockerfile:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
# copy parent/module POMs if the repo is multi-module
RUN mvn -f pom.xml -pl <module> -am -B -DskipTests dependency:go-offline
COPY <module>/src ./<module>/src
RUN mvn -f pom.xml -pl <module> -am -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/<module>/target/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring app.jar
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
CMD ["$JAVA_OPTS"]
```

Security tips:
- Never bake secrets into the Dockerfile. Use environment variables or secret managers (e.g., Kubernetes Secrets).
- Keep the healthcheck in place so the platform can monitor container health.

## Environment Variables

- Drive all environment-specific configuration via env vars (DB endpoints, credentials, object storage, port, etc.).
- In `application.yml`, rely on the `${ENV_VAR:default}` syntax for fallback values.

Common variables:
- `SERVER_PORT` (default 8080)
- `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_MOVIE_BUCKET`, `MINIO_IMAGE_BUCKET`
- `JAVA_OPTS` for JVM tuning inside the container

`application-dev.yml` can pin localhost endpoints (local MinIO, local Postgres) and enable `management.endpoint.health.show-details: always` for debugging.

## Spring / Actuator Configuration

- Include `spring-boot-starter-actuator`.
- Expose `health`, `info`, and `metrics` in production through `management.endpoints.web.exposure.include`. Add extras (`env`, `trace`) only for development.
- Adjust `management.endpoint.health.show-details` per profile to avoid leaking sensitive info in prod.

## Configuration Files

- `application.yml`: common configuration; optionally set `spring.profiles.active: dev` for local defaults.
- `application-dev.yml`: overrides referencing local env vars; keep secrets out of source control.
- Never commit credentials or tokens.

## Logging & Metrics

- Configure log levels in `application.yml`; leave `hibernate.show_sql` off in production.
- Actuator metrics come bundled; optionally wire Prometheus exporter if observability stack requires it.

## Testing Strategy

- Unit tests validate service logic, mappers, and validation rules.
- Integration tests: exercise persistence layer with Testcontainers or embedded DB when applicable.
- CI should run `mvn -T1C -B -DskipTests=false test`; fail the build on test failures.
- In Docker builds executed by CI, prefer keeping tests enabled (omit `-DskipTests`) to catch issues early.

## CI / CD Guidance

- Build via parent Maven module: `mvn -f pom.xml -pl <module> -am -B package`.
- Pipeline stages: build, unit tests, static analysis (SpotBugs, Checkstyle), container build, push image, deploy.
- Tag images with commit SHA + semantic version suffix.

## Security Practices

- Never commit secrets.
- Enforce TLS for external traffic in production.
- Scope MinIO/Postgres credentials with least privilege.

## Observability & Operations

- Expose `/actuator/health`, `/actuator/metrics`, and `/actuator/info`.
- Keep the Docker healthcheck hitting `/actuator/health` similar to `movie-service`.
- Add readiness probes when deploying to Kubernetes (e.g., verify DB connectivity).
- Align log format with platform expectations (JSON if shipping to ELK or similar).

## New Service Checklist

- [ ] Create Maven module with `artifactId = <service-name>` and register it in the parent `pom`.
- [ ] Create base package `com.pbl6.cinemate.<service>` plus the main `Application` bootstrap class.
- [ ] Add dependencies: web, validation, actuator, data-jpa (if Database), MapStruct/Lombok as needed.
- [ ] Prepare `application.yml` and `application-dev.yml` (only differing values between envs).
- [ ] Author a multi-stage Dockerfile (build + runtime) and include a `HEALTHCHECK`.
- [ ] Implement at least one controller, one service, request/response DTOs, plus validation.
- [ ] Write unit tests for service logic and validation-focused controller tests.
- [ ] Ensure the module builds via `mvn -pl <module> -am package`.
- [ ] Hook the module into the CI pipeline for build and test stages.
- [ ] Add a short module-level `README.md` describing endpoints and required env vars.

## Code Snippets

Controller skeleton:

```java
@RestController
@RequestMapping("/api/v1/<resource>")
public class ExampleController {
    private final ExampleService service;

    public ExampleController(ExampleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExampleResponse> create(@Valid @RequestBody ExampleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
```

Validation record example:

```java
public record ExampleCreateRequest(@NotBlank String name, @Min(1) int quantity) {}
```

Global exception handler snippet:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        // map field errors -> ApiError
    }
}
```

## Extra Suggestions

- Add custom `HealthIndicator`s per dependency (DB, external APIs, object storage).
- For upload/streaming workloads (like `movie-service`), adjust multipart limits and support chunked upload flows.
- For long-running tasks, consider a worker component instead of blocking web threads.

## Wrap-Up

This playbook provides the checklist and templates to launch a new service while honoring conventions already in place (per `movie-service`).

Files to create/verify before calling the service ready:
- `pom.xml` module
- `src/main/java/...` (controller/service/dto/config/exception packages)
- `src/main/resources/application.yml`, `application-dev.yml`
- `Dockerfile`
- `README.md` (module level)
