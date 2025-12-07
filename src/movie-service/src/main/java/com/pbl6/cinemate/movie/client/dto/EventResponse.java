package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Response from event tracking endpoints
 */
public record EventResponse(
        @JsonProperty("requestId") UUID requestId,

        @JsonProperty("status") String status,

        @JsonProperty("serverTimestamp") Instant serverTimestamp) {
    public boolean isAccepted() {
        return "accepted".equals(status);
    }

    public boolean isDuplicate() {
        return "duplicate".equals(status);
    }

    public boolean isFallback() {
        return "fallback".equals(status);
    }
}
