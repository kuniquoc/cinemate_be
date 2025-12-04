package com.pbl6.cinemate.streaming_signaling.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streaming")
public record SignalingProperties(Signaling signaling) {

    public record Signaling(
            @NotNull Duration redisTtlSegmentKeys,
            @NotNull Duration peerLastSeenTtl,
            @NotNull Duration peerMetricsTtl) {
    }
}
