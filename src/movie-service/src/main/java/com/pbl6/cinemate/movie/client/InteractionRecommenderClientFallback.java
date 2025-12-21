package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Fallback implementation for InteractionRecommenderClient
 * Used when the interaction-recommender-service is unavailable
 */
@Slf4j
@Component
public class InteractionRecommenderClientFallback implements InteractionRecommenderClient {

    @Override
    public EventResponse trackWatchEvent(WatchEventRequest request) {
        log.warn("Fallback: Failed to track watch event for user {}", request.userId());
        return createFallbackEventResponse(request.requestId());
    }

    @Override
    public EventResponse trackSearchEvent(SearchEventRequest request) {
        log.warn("Fallback: Failed to track search event for user {}", request.userId());
        return createFallbackEventResponse(request.requestId());
    }

    @Override
    public EventResponse trackRatingEvent(RatingEventRequest request) {
        log.warn("Fallback: Failed to track rating event for user {}", request.userId());
        return createFallbackEventResponse(request.requestId());
    }

    @Override
    public EventResponse trackFavoriteEvent(FavoriteEventRequest request) {
        log.warn("Fallback: Failed to track favorite event for user {}", request.userId());
        return createFallbackEventResponse(request.requestId());
    }

    @Override
    public RecommendationResponse getRecommendations(UUID userId, Integer k, String context, Boolean retrain) {
        log.warn("Fallback: Failed to get recommendations for user {}", userId);
        return new RecommendationResponse(
                userId,
                "fallback",
                false,
                Collections.emptyList(),
                Instant.now(),
                context);
    }

    @Override
    public UserFeaturesResponse getUserFeatures(UUID userId) {
        log.warn("Fallback: Failed to get features for user {}", userId);
        return new UserFeaturesResponse(
                userId,
                Map.of(),
                "fallback",
                Instant.now());
    }

    @Override
    public HealthResponse getHealth() {
        log.warn("Fallback: Interaction recommender service unavailable");
        return new HealthResponse(
                "unavailable",
                "fallback",
                Instant.now(),
                Map.of());
    }

    private EventResponse createFallbackEventResponse(UUID requestId) {
        return new EventResponse(
                requestId != null ? requestId : UUID.randomUUID(),
                "fallback",
                Instant.now());
    }
}
