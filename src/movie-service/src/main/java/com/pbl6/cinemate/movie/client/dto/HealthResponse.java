package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Response from health endpoint
 */
public record HealthResponse(
        @JsonProperty("status") String status,

        @JsonProperty("version") String version,

        @JsonProperty("timestamp") Instant timestamp,

        @JsonProperty("components") Map<String, ComponentHealth> components) {
    public boolean isHealthy() {
        return "healthy".equals(status);
    }

    public boolean isDegraded() {
        return "degraded".equals(status);
    }

    public record ComponentHealth(
            @JsonProperty("status") String status,

            @JsonProperty("latencyMs") Double latencyMs,

            @JsonProperty("message") String message) {
    }
}
