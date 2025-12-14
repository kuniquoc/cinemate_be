package com.pbl6.cinemate.streaming_signaling.service;

import com.pbl6.cinemate.shared.streaming.StreamingRedisKeys;
import com.pbl6.cinemate.streaming_signaling.config.SignalingProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class PeerMetricsService {

    private static final Logger log = LoggerFactory.getLogger(PeerMetricsService.class);
    private static final String FIELD_TOTAL_SEGMENTS = "totalSegments";
    private static final String FIELD_PEER_SUCCESS_SEGMENTS = "peerSuccessSegments";
    private static final String FIELD_SUCCESS_RATE = "successRate";
    private static final String FIELD_LAST_ACTIVE = "lastActive";
    private static final String CLIENT_ID_REQUIRED_MESSAGE = "clientId must not be null";
    private static final String PEER_METRICS_TTL_REQUIRED = "Peer metrics TTL must not be null";
    private final StringRedisTemplate redisTemplate;
    private final SignalingProperties properties;
    private final Clock clock;

    public PeerMetricsService(StringRedisTemplate redisTemplate, SignalingProperties properties, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    public double updateReliability(@NonNull String clientId, String source) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED_MESSAGE);
        String metricsKey = StreamingRedisKeys.peerMetricsKey(sanitizedClientId);
        if (metricsKey == null) {
            log.warn("Metrics key is null for clientId: {}", sanitizedClientId);
            return 0.0;
        }
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        long totalSegments = nullSafeLong(hashOps.increment(metricsKey, FIELD_TOTAL_SEGMENTS, 1L));
        if (totalSegments < 1) {
            totalSegments = 1;
        }

        boolean successfulPeerTransfer = source == null || !"origin".equalsIgnoreCase(source);
        long successfulSegments = successfulPeerTransfer
                ? nullSafeLong(hashOps.increment(metricsKey, FIELD_PEER_SUCCESS_SEGMENTS, 1L))
                : getLongField(metricsKey, FIELD_PEER_SUCCESS_SEGMENTS);

        double successRate = successfulSegments <= 0 ? 0.0 : successfulSegments / (double) totalSegments;
        String successRateValue = String.valueOf(successRate);
        String lastActiveValue = String.valueOf(clock.instant().getEpochSecond());
        hashOps.put(metricsKey, FIELD_SUCCESS_RATE, Objects.requireNonNull(successRateValue));
        hashOps.put(metricsKey, FIELD_LAST_ACTIVE, Objects.requireNonNull(lastActiveValue));

        Duration ttl = Objects.requireNonNull(
                properties.signaling().peerMetricsTtl(),
                PEER_METRICS_TTL_REQUIRED);
        redisTemplate.expire(metricsKey, ttl);

        log.debug(
                "Updated reliability for client {} (source={}): total={}, success={}, rate={}",
                sanitizedClientId,
                source,
                totalSegments,
                successfulSegments,
                successRate);
        return successRate;
    }

    public Map<Object, Object> loadAll(@NonNull String clientId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED_MESSAGE);
        String metricsKey = StreamingRedisKeys.peerMetricsKey(sanitizedClientId);
        if (metricsKey == null) {
            log.warn("Metrics key is null for clientId: {}", sanitizedClientId);
            return Collections.emptyMap();
        }
        return redisTemplate.opsForHash().entries(metricsKey);
    }

    public long markLastActive(@NonNull String clientId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED_MESSAGE);
        long epochSecond = Instant.now(clock).getEpochSecond();
        String lastActiveValue = String.valueOf(epochSecond);
        String metricsKey = StreamingRedisKeys.peerMetricsKey(sanitizedClientId);
        if (metricsKey == null || lastActiveValue == null) {
            log.warn("Metrics key or lastActiveValue is null for clientId: {}", sanitizedClientId);
        } else {
            log.debug("Marking last active for client {}: {}", sanitizedClientId, lastActiveValue);
            redisTemplate.opsForHash().put(metricsKey, FIELD_LAST_ACTIVE, lastActiveValue);

            Duration ttl = Objects.requireNonNull(
                    properties.signaling().peerMetricsTtl(),
                    PEER_METRICS_TTL_REQUIRED);
            redisTemplate.expire(metricsKey, ttl);
        }

        return epochSecond;
    }

    private long getLongField(@NonNull String key, @NonNull String hashKey) {
        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            log.warn("Unexpected number format for {}#{}: {}", key, hashKey, value, ex);
            return 0L;
        }
    }

    private long nullSafeLong(Long value) {
        return value != null ? value : 0L;
    }
}
