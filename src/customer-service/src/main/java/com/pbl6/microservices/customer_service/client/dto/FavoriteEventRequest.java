package com.pbl6.microservices.customer_service.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record FavoriteEventRequest(
        @JsonProperty("requestId") UUID requestId,
        @JsonProperty("userId") UUID userId,
        @JsonProperty("movieId") UUID movieId,
        @JsonProperty("clientTimestamp") Instant clientTimestamp,
        @JsonProperty("metadata") Metadata metadata) {
    public static FavoriteEventRequest create(UUID userId, UUID movieId, String action) {
        return new FavoriteEventRequest(
                UUID.randomUUID(),
                userId,
                movieId,
                Instant.now(),
                new Metadata(action));
    }

    public record Metadata(@JsonProperty("action") String action) {
    }
}
