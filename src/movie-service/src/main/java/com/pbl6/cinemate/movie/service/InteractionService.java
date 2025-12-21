package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.client.dto.RecommendationResponse;
import com.pbl6.cinemate.movie.dto.response.MovieWithScoreResponse;
import com.pbl6.cinemate.movie.dto.response.RecommendedMoviesResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service for getting movie recommendations
 * Wraps InteractionRecommenderClient and enriches with movie details
 */
public interface InteractionService {

    // ============== Recommendations ==============

    /**
     * Get personalized movie recommendations with full movie details
     */
    List<MovieWithScoreResponse> getRecommendedMovies(UUID userId, int count, String context);

    /**
     * Get home page recommendations with full movie details
     */
    List<MovieWithScoreResponse> getHomeRecommendations(UUID userId, int count);

    /**
     * Get similar movies recommendations
     */
    List<MovieWithScoreResponse> getSimilarMovies(UUID userId, UUID movieId, int count);

    /**
     * Get raw recommendation response (movie IDs only)
     */
    RecommendationResponse getRawRecommendations(UUID userId, int count, String context);

    // ============== Health ==============

    /**
     * Check if recommendation service is healthy
     */
    boolean isServiceHealthy();
}
