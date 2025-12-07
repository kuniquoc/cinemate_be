package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Request for tracking search events
 */
public record SearchEventRequest(
        @JsonProperty("requestId") UUID requestId,

        @JsonProperty("userId") UUID userId,

        @JsonProperty("movieId") UUID movieId,

        @JsonProperty("clientTimestamp") Instant clientTimestamp,

        @JsonProperty("metadata") SearchMetadata metadata) {
    public record SearchMetadata(
            @JsonProperty("query") String query,

            @JsonProperty("resultsCount") Integer resultsCount,

            @JsonProperty("filters") Map<String, Object> filters,

            @JsonProperty("sessionId") String sessionId) {
    }

    public static SearchEventRequest create(
            UUID userId,
            String query,
            Integer resultsCount,
            Map<String, Object> filters,
            String sessionId) {
        return new SearchEventRequest(
                UUID.randomUUID(),
                userId,
                null,
                Instant.now(),
                new SearchMetadata(query, resultsCount, filters, sessionId));
    }

    public static SearchEventRequest createWithClickedMovie(
            UUID userId,
            UUID clickedMovieId,
            String query,
            Integer resultsCount) {
        return new SearchEventRequest(
                UUID.randomUUID(),
                userId,
                clickedMovieId,
                Instant.now(),
                new SearchMetadata(query, resultsCount, null, null));
    }
}
