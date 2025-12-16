package com.pbl6.cinemate.movie.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request for submitting recommendation feedback
 */
public record FeedbackRequest(
        @JsonProperty("userId") UUID userId,

        @JsonProperty("modelVersion") String modelVersion,

        @JsonProperty("impressionList") List<UUID> impressionList,

        @JsonProperty("clickedItemId") UUID clickedItemId,

        @JsonProperty("watchTimeSec") Integer watchTimeSec,

        @JsonProperty("timestamp") Instant timestamp,

        @JsonProperty("context") String context) {
    public static FeedbackRequest create(
            UUID userId,
            String modelVersion,
            List<UUID> impressionList,
            UUID clickedItemId,
            Integer watchTimeSec,
            String context) {
        return new FeedbackRequest(
                userId,
                modelVersion,
                impressionList,
                clickedItemId,
                watchTimeSec,
                Instant.now(),
                context);
    }

    public static FeedbackRequest createClickFeedback(
            UUID userId,
            String modelVersion,
            List<UUID> impressionList,
            UUID clickedItemId,
            String context) {
        return create(userId, modelVersion, impressionList, clickedItemId, null, context);
    }

    public static FeedbackRequest createImpressionFeedback(
            UUID userId,
            String modelVersion,
            List<UUID> impressionList,
            String context) {
        return create(userId, modelVersion, impressionList, null, null, context);
    }
}
