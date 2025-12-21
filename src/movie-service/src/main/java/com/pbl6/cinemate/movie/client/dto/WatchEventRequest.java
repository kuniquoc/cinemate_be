package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Request for tracking watch events
 */
public record WatchEventRequest(
                @JsonProperty("requestId") UUID requestId,

                @JsonProperty("userId") UUID userId,

                @JsonProperty("movieId") UUID movieId,

                @JsonProperty("clientTimestamp") Instant clientTimestamp,

                @JsonProperty("metadata") WatchMetadata metadata) {
        public static WatchEventRequest create(
                        UUID userId,
                        UUID movieId,
                        Integer watchDuration,
                        String device,
                        String quality,
                        String sessionId,
                        Double progressPercent) {
                return new WatchEventRequest(
                                UUID.randomUUID(),
                                userId,
                                movieId,
                                Instant.now(),
                                new WatchMetadata(watchDuration, device, quality, sessionId, progressPercent));
        }

        public record WatchMetadata(
                        @JsonProperty("watchDuration") Integer watchDuration,

                        @JsonProperty("device") String device,

                        @JsonProperty("quality") String quality,

                        @JsonProperty("sessionId") String sessionId,

                        @JsonProperty("progressPercent") Double progressPercent) {
        }
}
