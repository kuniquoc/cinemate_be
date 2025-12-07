package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Response from feature refresh endpoint
 */
public record FeatureRefreshResponse(
        @JsonProperty("userId") UUID userId,

        @JsonProperty("status") String status,

        @JsonProperty("featureCount") Integer featureCount,

        @JsonProperty("refreshedAt") Instant refreshedAt) {
    public boolean isSuccessful() {
        return "refreshed".equals(status);
    }
}
