package com.pbl6.cinemate.movie.dto.response;

import com.pbl6.cinemate.movie.client.dto.RecommendationResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing recommended movies with full details
 */
public record RecommendedMoviesResponse(
        UUID userId,
        String modelVersion,
        boolean cached,
        List<RecommendedMovieItem> recommendations,
        Instant generatedAt,
        String context,
        int totalCount) {
    /**
     * Create from raw recommendation response and movie list
     */
    public static RecommendedMoviesResponse from(
            RecommendationResponse rawResponse,
            List<RecommendedMovieItem> items) {
        return new RecommendedMoviesResponse(
                rawResponse.userId(),
                rawResponse.modelVersion(),
                rawResponse.cached(),
                items,
                rawResponse.generatedAt(),
                rawResponse.context(),
                items.size());
    }

    /**
     * Create empty response
     */
    public static RecommendedMoviesResponse empty(UUID userId, String context) {
        return new RecommendedMoviesResponse(
                userId,
                "none",
                false,
                List.of(),
                Instant.now(),
                context,
                0);
    }

    /**
     * Single recommended movie with details and score
     */
    public record RecommendedMovieItem(
            MovieResponse movie,
            Double score,
            List<String> reasons) {
    }
}
