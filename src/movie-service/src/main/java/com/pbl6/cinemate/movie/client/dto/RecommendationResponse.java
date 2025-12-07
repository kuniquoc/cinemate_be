package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response from recommendation endpoint
 */
public record RecommendationResponse(
        @JsonProperty("userId") UUID userId,

        @JsonProperty("modelVersion") String modelVersion,

        @JsonProperty("cached") boolean cached,

        @JsonProperty("recommendations") List<RecommendationItem> recommendations,

        @JsonProperty("generatedAt") Instant generatedAt,

        @JsonProperty("context") String context) {
    public record RecommendationItem(
            @JsonProperty("movieId") UUID movieId,

            @JsonProperty("score") Double score,

            @JsonProperty("reasons") List<String> reasons) {
    }

    /**
     * Get list of recommended movie IDs
     */
    public List<UUID> getMovieIds() {
        return recommendations.stream()
                .map(RecommendationItem::movieId)
                .toList();
    }

    /**
     * Check if recommendations are empty
     */
    public boolean isEmpty() {
        return recommendations == null || recommendations.isEmpty();
    }
}
