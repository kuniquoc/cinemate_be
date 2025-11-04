package com.pbl6.cinemate.streaming.signaling.service;

import com.pbl6.cinemate.shared.streaming.StreamingRedisKeys;
import com.pbl6.cinemate.streaming.signaling.config.SignalingProperties;
import com.pbl6.cinemate.streaming.signaling.dto.PeerInfo;
import com.pbl6.cinemate.streaming.signaling.dto.PeerListMessage;
import com.pbl6.cinemate.streaming.signaling.dto.PeerMetrics;
import com.pbl6.cinemate.streaming.signaling.dto.ReportSegmentAckMessage;
import com.pbl6.cinemate.streaming.signaling.dto.WhoHasReplyMessage;
import com.pbl6.cinemate.streaming.signaling.metrics.PeerMetricsService;
import com.pbl6.cinemate.streaming.signaling.messaging.StreamEventSubscriber;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class SignalingService {

    private static final Logger log = LoggerFactory.getLogger(SignalingService.class);
    private static final String CLIENT_ID_REQUIRED = "clientId must not be null";
    private static final String STREAM_ID_REQUIRED = "streamId must not be null";
    private static final String SEGMENT_ID_REQUIRED = "segmentId must not be null";
    private static final String SOURCE_REQUIRED = "source must not be null";
    private static final String SEGMENT_TTL_REQUIRED = "Segment TTL must not be null";
    private static final String LAST_SEEN_TTL_REQUIRED = "Peer last seen TTL must not be null";
    private static final String KEY_COMMANDS_REQUIRED = "Redis key commands must not be null";
    private static final String SCAN_CURSOR_REQUIRED = "Redis scan cursor must not be null";
    private static final String PEER_KEY_NULL = "Peer key is null for streamId: {}";
    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final SignalingProperties properties;
    private final PeerMetricsService peerMetricsService;
    private final StreamEventSubscriber eventSubscriber;
    private final Clock clock;
    private final Map<String, String> activeClients = new ConcurrentHashMap<>();

    public SignalingService(
            StringRedisTemplate redisTemplate,
            RedisConnectionFactory connectionFactory,
            SignalingProperties properties,
            PeerMetricsService peerMetricsService,
            StreamEventSubscriber eventSubscriber,
            Clock clock) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "signalingProperties must not be null");
        this.peerMetricsService = Objects.requireNonNull(peerMetricsService, "peerMetricsService must not be null");
        this.eventSubscriber = Objects.requireNonNull(eventSubscriber, "eventSubscriber must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PeerListMessage registerClient(@NonNull String clientId, @NonNull String streamId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedStreamId = Objects.requireNonNull(streamId, STREAM_ID_REQUIRED);
        log.info("Client {} connected for stream {}", sanitizedClientId, sanitizedStreamId);
        activeClients.put(sanitizedClientId, sanitizedStreamId);
        eventSubscriber.ensureSubscribed(sanitizedStreamId);
        touchLastSeen(sanitizedClientId);
        String peerKey = StreamingRedisKeys.streamPeersKey(sanitizedStreamId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL, sanitizedStreamId);
            return new PeerListMessage(sanitizedStreamId, Collections.emptySet());
        }
        Set<String> peers = Optional
                .ofNullable(redisTemplate.opsForSet().members(peerKey))
                .orElseGet(Collections::emptySet);
        return new PeerListMessage(sanitizedStreamId, peers);
    }

    public WhoHasReplyMessage handleWhoHas(@NonNull String streamId, @NonNull String segmentId) {
        String sanitizedStreamId = Objects.requireNonNull(streamId, STREAM_ID_REQUIRED);
        String sanitizedSegmentId = Objects.requireNonNull(segmentId, SEGMENT_ID_REQUIRED);
        String segmentKey = StreamingRedisKeys.segmentOwnersKey(sanitizedStreamId, sanitizedSegmentId);
        if (segmentKey == null) {
            log.warn(
                    "Segment key is null for streamId: {} segmentId: {}",
                    sanitizedStreamId,
                    sanitizedSegmentId);
            return new WhoHasReplyMessage(sanitizedSegmentId, List.of());
        }
        Set<String> peerIds = Optional
                .ofNullable(redisTemplate.opsForSet().members(segmentKey))
                .orElseGet(Collections::emptySet);

        if (peerIds.isEmpty()) {
            log.debug("No peers found for stream {} segment {}", sanitizedStreamId, sanitizedSegmentId);
            return new WhoHasReplyMessage(sanitizedSegmentId, List.of());
        }

        List<PeerInfo> peerInfos = new ArrayList<>(peerIds.size());
        for (String peerId : peerIds) {
            if (peerId == null) {
                continue;
            }
            Map<Object, Object> metricsData = peerMetricsService.loadAll(peerId);
            PeerMetrics metrics = mapMetrics(metricsData);
            peerInfos.add(new PeerInfo(peerId, metrics));
        }
        log.debug("Found {} peers for stream {} segment {}", peerInfos.size(), sanitizedStreamId, sanitizedSegmentId);
        return new WhoHasReplyMessage(sanitizedSegmentId, peerInfos);
    }

    public ReportSegmentAckMessage handleReportSegment(
            @NonNull String clientId,
            @NonNull String streamId,
            @NonNull String segmentId,
            @NonNull String source,
            double speed,
            long latency) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedStreamId = Objects.requireNonNull(streamId, STREAM_ID_REQUIRED);
        String sanitizedSegmentId = Objects.requireNonNull(segmentId, SEGMENT_ID_REQUIRED);
        String sanitizedSource = Objects.requireNonNull(source, SOURCE_REQUIRED);

        String segmentKey = StreamingRedisKeys.segmentOwnersKey(sanitizedStreamId, sanitizedSegmentId);
        if (segmentKey == null) {
            log.warn(
                    "Segment key is null for streamId: {} segmentId: {}",
                    sanitizedStreamId,
                    sanitizedSegmentId);
            return new ReportSegmentAckMessage(sanitizedSegmentId);
        }
        Duration ttl = Objects.requireNonNull(
                properties.getSignaling().getRedisTtlSegmentKeys(),
                SEGMENT_TTL_REQUIRED);
        redisTemplate.opsForSet().add(segmentKey, sanitizedClientId);
        redisTemplate.expire(segmentKey, ttl);

        String peerKey = StreamingRedisKeys.streamPeersKey(sanitizedStreamId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL, sanitizedStreamId);
            return new ReportSegmentAckMessage(sanitizedSegmentId);
        }
        redisTemplate.opsForSet().add(peerKey, sanitizedClientId);
        redisTemplate.expire(peerKey, ttl);

        double successRate = peerMetricsService.updateReliability(sanitizedClientId, sanitizedSource);
        long lastActive = peerMetricsService.markLastActive(sanitizedClientId);

        String metricsKey = StreamingRedisKeys.peerMetricsKey(sanitizedClientId);
        if (metricsKey == null) {
            log.warn("Metrics key is null for clientId: {}", sanitizedClientId);
            return new ReportSegmentAckMessage(sanitizedSegmentId);
        }
        String uploadSpeedValue = Double.toString(speed);
        String latencyValue = Long.toString(latency);
        String successRateValue = Double.toString(successRate);
        String lastActiveValue = Long.toString(lastActive);
        redisTemplate.opsForHash().put(metricsKey, "uploadSpeed", Objects.requireNonNull(uploadSpeedValue));
        redisTemplate.opsForHash().put(metricsKey, "latency", Objects.requireNonNull(latencyValue));
        redisTemplate.opsForHash().put(metricsKey, "successRate", Objects.requireNonNull(successRateValue));
        redisTemplate.opsForHash().put(metricsKey, "lastActive", Objects.requireNonNull(lastActiveValue));

        log.info(
                "[Metrics] {} now has {} (stream={}, latency={}ms, speed={}MB/s)",
                sanitizedClientId,
                sanitizedSegmentId,
                sanitizedStreamId,
                latency,
                speed);
        return new ReportSegmentAckMessage(sanitizedSegmentId);
    }

    public void handleDisconnect(@NonNull String clientId, @NonNull String streamId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedStreamId = Objects.requireNonNull(streamId, STREAM_ID_REQUIRED);
        log.info("Client {} disconnected from {}", sanitizedClientId, sanitizedStreamId);
        activeClients.remove(sanitizedClientId);
        removeClientFromSegments(sanitizedClientId, sanitizedStreamId);
        String peerKey = StreamingRedisKeys.streamPeersKey(sanitizedStreamId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL, sanitizedStreamId);
            return;
        }
        redisTemplate.opsForSet().remove(peerKey, sanitizedClientId);
    }

    private void removeClientFromSegments(@NonNull String clientId, @NonNull String streamId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedStreamId = Objects.requireNonNull(streamId, STREAM_ID_REQUIRED);
        String pattern = StreamingRedisKeys.segmentOwnersKey(sanitizedStreamId, "*");
        if (pattern == null) {
            log.warn("Segment owners key pattern is null for streamId: {}", sanitizedStreamId);
            return;
        }
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(128).build();
        try (RedisConnection connection = connectionFactory.getConnection();
                Cursor<byte[]> cursor = Objects.requireNonNull(
                        Objects.requireNonNull(connection.keyCommands(), KEY_COMMANDS_REQUIRED)
                                .scan(scanOptions),
                        SCAN_CURSOR_REQUIRED)) {
            while (cursor.hasNext()) {
                byte[] rawKey = cursor.next();
                if (rawKey == null) {
                    continue;
                }
                String key = new String(rawKey, StandardCharsets.UTF_8);
                redisTemplate.opsForSet().remove(key, sanitizedClientId);
            }
        } catch (DataAccessResourceFailureException ex) {
            log.warn("Failed to scan Redis keys for stream {}: {}", sanitizedStreamId, ex.getMessage());
        }
    }

    private PeerMetrics mapMetrics(Map<Object, Object> metricsData) {
        Map<Object, Object> safeData = metricsData != null ? metricsData : Collections.emptyMap();
        double uploadSpeed = parseDouble(safeData.get("uploadSpeed"), 0.0);
        int latency = (int) parseDouble(safeData.get("latency"), 999.0);
        double successRate = parseDouble(safeData.get("successRate"), 0.5);
        long lastActive = parseLong(safeData.get("lastActive"), 0L);
        if (lastActive == 0L) {
            lastActive = Instant.now(clock).getEpochSecond();
        }
        return new PeerMetrics(uploadSpeed, latency, successRate, lastActive);
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void touchLastSeen(@NonNull String clientId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        Duration ttl = Objects.requireNonNull(
                properties.getSignaling().getPeerLastSeenTtl(),
                LAST_SEEN_TTL_REQUIRED);
        String key = StreamingRedisKeys.peerLastSeenKey(sanitizedClientId);
        if (key == null) {
            log.warn("Last seen key is null for clientId: {}", sanitizedClientId);
            return;
        }
        String lastSeenValue = Long.toString(Instant.now(clock).getEpochSecond());
        redisTemplate.opsForValue().set(key, Objects.requireNonNull(lastSeenValue), ttl);
    }
}
