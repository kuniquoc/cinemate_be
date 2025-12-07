package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Request for tracking favorite events
 */
public record FavoriteEventRequest(
    @JsonProperty("requestId")
    UUID requestId,
    
    @JsonProperty("userId")
    UUID userId,
    
    @JsonProperty("movieId")
    UUID movieId,
    
    @JsonProperty("clientTimestamp")
    Instant clientTimestamp,
    
    @JsonProperty("metadata")
    FavoriteMetadata metadata
) {
    public record FavoriteMetadata(
        @JsonProperty("action")
        String action,
        
        @JsonProperty("listId")
        String listId
    ) {}
    
    public static FavoriteEventRequest createAddFavorite(UUID userId, UUID movieId) {
        return new FavoriteEventRequest(
            UUID.randomUUID(),
            userId,
            movieId,
            Instant.now(),
            new FavoriteMetadata("add", null)
        );
    }
    
    public static FavoriteEventRequest createRemoveFavorite(UUID userId, UUID movieId) {
        return new FavoriteEventRequest(
            UUID.randomUUID(),
            userId,
            movieId,
            Instant.now(),
            new FavoriteMetadata("remove", null)
        );
    }
    
    public static FavoriteEventRequest createWithList(UUID userId, UUID movieId, String action, String listId) {
        return new FavoriteEventRequest(
            UUID.randomUUID(),
            userId,
            movieId,
            Instant.now(),
            new FavoriteMetadata(action, listId)
        );
    }
}
