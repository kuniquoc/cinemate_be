package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for Interaction Recommender Service
 * Handles user interaction tracking and movie recommendations
 */
@FeignClient(name = "interaction-recommender-service", url = "${interaction.recommender.service.url:http://interaction-recommender-service:8000}", fallback = InteractionRecommenderClientFallback.class)
public interface InteractionRecommenderClient {

        // ============== Event Tracking ==============

        /**
         * Track watch event when user watches a movie
         */
        @PostMapping("/api/v1/events/watch")
        EventResponse trackWatchEvent(@RequestBody WatchEventRequest request);

        /**
         * Track search event when user performs a search
         */
        @PostMapping("/api/v1/events/search")
        EventResponse trackSearchEvent(@RequestBody SearchEventRequest request);

        /**
         * Track rating event when user rates a movie
         */
        @PostMapping("/api/v1/events/rating")
        EventResponse trackRatingEvent(@RequestBody RatingEventRequest request);

        /**
         * Track favorite event when user adds/removes movie from favorites
         */
        @PostMapping("/api/v1/events/favorite")
        EventResponse trackFavoriteEvent(@RequestBody FavoriteEventRequest request);

        // ============== Recommendations ==============

        /**
         * Get personalized movie recommendations for a user
         *
         * @param userId  User identifier
         * @param k       Number of recommendations (default: 20)
         * @param context Context for recommendations (home, detail, search, similar)
         */
        @GetMapping("/api/v1/recommend/{userId}")
        RecommendationResponse getRecommendations(
                        @PathVariable("userId") UUID userId,
                        @RequestParam(name = "k", required = false, defaultValue = "20") Integer k,
                        @RequestParam(name = "context", required = false, defaultValue = "home") String context);

        // ============== Features ==============

        /**
         * Get user features
         */
        @GetMapping("/api/v1/features/{userId}")
        UserFeaturesResponse getUserFeatures(@PathVariable("userId") UUID userId);

        /**
         * Refresh user features from historical events
         */
        @PostMapping("/api/v1/features/{userId}/refresh")
        FeatureRefreshResponse refreshUserFeatures(
                        @PathVariable("userId") UUID userId,
                        @RequestParam(name = "days_back", required = false, defaultValue = "90") Integer daysBack);

        // ============== Feedback ==============

        /**
         * Submit user feedback on recommendations
         */
        @PostMapping("/api/v1/feedback")
        FeedbackResponse submitFeedback(@RequestBody FeedbackRequest request);

        // ============== Health ==============

        /**
         * Check service health
         */
        @GetMapping("/api/v1/health")
        HealthResponse getHealth();
}
