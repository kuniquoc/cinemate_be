package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Response from feedback endpoint
 */
public record FeedbackResponse(
        @JsonProperty("feedbackId") UUID feedbackId,

        @JsonProperty("status") String status,

        @JsonProperty("processedAt") Instant processedAt) {
    public boolean isRecorded() {
        return "recorded".equals(status);
    }
}
