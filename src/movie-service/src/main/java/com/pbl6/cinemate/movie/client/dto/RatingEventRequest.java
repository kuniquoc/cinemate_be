package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Request for tracking rating events
 */
public record RatingEventRequest(
        @JsonProperty("requestId") UUID requestId,

        @JsonProperty("userId") UUID userId,

        @JsonProperty("movieId") UUID movieId,

        @JsonProperty("clientTimestamp") Instant clientTimestamp,

        @JsonProperty("metadata") RatingMetadata metadata) {
    public static RatingEventRequest create(
            UUID userId,
            UUID movieId,
            Double rating,
            Double previousRating) {
        return new RatingEventRequest(
                UUID.randomUUID(),
                userId,
                movieId,
                Instant.now(),
                new RatingMetadata(rating, previousRating));
    }

    public static RatingEventRequest create(
            UUID userId,
            UUID movieId,
            Double rating) {
        return create(userId, movieId, rating, null);
    }

    public record RatingMetadata(
            @JsonProperty("rating") Double rating,

            @JsonProperty("previousRating") Double previousRating) {
    }
}
