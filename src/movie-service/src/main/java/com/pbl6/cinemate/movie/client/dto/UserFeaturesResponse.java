package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response from user features endpoint
 */
public record UserFeaturesResponse(
        @JsonProperty("userId") UUID userId,

        @JsonProperty("features") Map<String, Object> features,

        @JsonProperty("version") String version,

        @JsonProperty("updatedAt") Instant updatedAt) {
    /**
     * Get a specific feature value
     */
    @SuppressWarnings("unchecked")
    public <T> T getFeature(String key, Class<T> type) {
        Object value = features.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Get total interactions count
     */
    public Integer getTotalInteractions() {
        return getFeature("totalInteractions", Integer.class);
    }

    /**
     * Get average rating
     */
    public Double getAverageRating() {
        return getFeature("avgRating", Double.class);
    }
}
