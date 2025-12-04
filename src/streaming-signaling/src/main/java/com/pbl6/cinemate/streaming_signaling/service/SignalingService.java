package com.pbl6.cinemate.streaming_signaling.service;

import com.pbl6.cinemate.shared.streaming.StreamingRedisKeys;
import com.pbl6.cinemate.streaming_signaling.config.SignalingProperties;
import com.pbl6.cinemate.streaming_signaling.dto.PeerInfo;
import com.pbl6.cinemate.streaming_signaling.dto.PeerListMessage;
import com.pbl6.cinemate.streaming_signaling.dto.PeerMetrics;
import com.pbl6.cinemate.streaming_signaling.dto.ReportSegmentAckMessage;
import com.pbl6.cinemate.streaming_signaling.dto.WhoHasReplyMessage;

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
    private static final String MOVIE_ID_REQUIRED = "movieId must not be null";
    private static final String SEGMENT_ID_REQUIRED = "segmentId must not be null";
    private static final String SOURCE_REQUIRED = "source must not be null";
    private static final String SEGMENT_TTL_REQUIRED = "Segment TTL must not be null";
    private static final String LAST_SEEN_TTL_REQUIRED = "Peer last seen TTL must not be null";
    private static final String KEY_COMMANDS_REQUIRED = "Redis key commands must not be null";
    private static final String SCAN_CURSOR_REQUIRED = "Redis scan cursor must not be null";
    private static final String PEER_KEY_NULL_FOR_MOVIE = "Peer key is null for movieId: {}";
    private static final String SEGMENT_KEY_NULL = "Segment key is null for movieId: {} qualityId: {} segmentId: {}";
    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final SignalingProperties properties;
    private final PeerMetricsService peerMetricsService;
    private final Clock clock;
    private final Map<String, String> activeClients = new ConcurrentHashMap<>();

    public SignalingService(
            StringRedisTemplate redisTemplate,
            RedisConnectionFactory connectionFactory,
            SignalingProperties properties,
            PeerMetricsService peerMetricsService,
            Clock clock) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "signalingProperties must not be null");
        this.peerMetricsService = Objects.requireNonNull(peerMetricsService, "peerMetricsService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Registers a client for a movie stream (movieId = streamId for ABR).
     * Returns list of peers watching the same movie (across all qualities).
     */
    public PeerListMessage registerClient(@NonNull String clientId, @NonNull String movieId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);
        log.info("Client {} connected for movie {}", sanitizedClientId, sanitizedMovieId);
        activeClients.put(sanitizedClientId, sanitizedMovieId);
        touchLastSeen(sanitizedClientId);

        String peerKey = StreamingRedisKeys.moviePeersKey(sanitizedMovieId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL_FOR_MOVIE, sanitizedMovieId);
            return new PeerListMessage(sanitizedMovieId, Collections.emptySet());
        }

        redisTemplate.opsForSet().add(peerKey, sanitizedClientId);
        Duration ttl = Objects.requireNonNull(
                properties.signaling().redisTtlSegmentKeys(),
                SEGMENT_TTL_REQUIRED);
        redisTemplate.expire(peerKey, ttl);

        Set<String> peers = Optional
                .ofNullable(redisTemplate.opsForSet().members(peerKey))
                .orElseGet(Collections::emptySet);
        return new PeerListMessage(sanitizedMovieId, peers);
    }

    /**
     * Finds peers with a specific segment (with quality information).
     * 
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param segmentId the segment identifier
     */
    public WhoHasReplyMessage handleWhoHas(@NonNull String movieId, String qualityId, @NonNull String segmentId) {
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);
        String sanitizedSegmentId = Objects.requireNonNull(segmentId, SEGMENT_ID_REQUIRED);

        String segmentKey = StreamingRedisKeys.segmentOwnersKey(sanitizedMovieId, qualityId, sanitizedSegmentId);
        if (segmentKey == null) {
            log.warn(SEGMENT_KEY_NULL, sanitizedMovieId, qualityId, sanitizedSegmentId);
            return new WhoHasReplyMessage(qualityId, sanitizedSegmentId, List.of());
        }

        Set<String> peerIds = Optional
                .ofNullable(redisTemplate.opsForSet().members(segmentKey))
                .orElseGet(Collections::emptySet);

        if (peerIds.isEmpty()) {
            log.debug("No peers found for movie {} quality {} segment {}",
                    sanitizedMovieId, qualityId, sanitizedSegmentId);
            return new WhoHasReplyMessage(qualityId, sanitizedSegmentId, List.of());
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
        log.debug("Found {} peers for movie {} quality {} segment {}",
                peerInfos.size(), sanitizedMovieId, qualityId, sanitizedSegmentId);
        return new WhoHasReplyMessage(qualityId, sanitizedSegmentId, peerInfos);
    }

    /**
     * Reports that a client has successfully obtained a segment.
     * 
     * @param clientId  the client identifier
     * @param movieId   the movie identifier
     * @param qualityId the quality variant
     * @param segmentId the segment identifier
     */
    public ReportSegmentAckMessage handleReportSegment(
            @NonNull String clientId,
            @NonNull String movieId,
            String qualityId,
            @NonNull String segmentId,
            @NonNull String source,
            double speed,
            long latency) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);
        String sanitizedSegmentId = Objects.requireNonNull(segmentId, SEGMENT_ID_REQUIRED);
        String sanitizedSource = Objects.requireNonNull(source, SOURCE_REQUIRED);

        String segmentKey = StreamingRedisKeys.segmentOwnersKey(sanitizedMovieId, qualityId, sanitizedSegmentId);
        if (segmentKey == null) {
            log.warn(SEGMENT_KEY_NULL, sanitizedMovieId, qualityId, sanitizedSegmentId);
            return new ReportSegmentAckMessage(sanitizedSegmentId);
        }

        Duration ttl = Objects.requireNonNull(
                properties.signaling().redisTtlSegmentKeys(),
                SEGMENT_TTL_REQUIRED);
        redisTemplate.opsForSet().add(segmentKey, sanitizedClientId);
        redisTemplate.expire(segmentKey, ttl);

        String peerKey = StreamingRedisKeys.moviePeersKey(sanitizedMovieId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL_FOR_MOVIE, sanitizedMovieId);
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

        log.info("[Metrics] {} now has {} (movie={}, quality={}, latency={}ms, speed={}MB/s)",
                sanitizedClientId, sanitizedSegmentId, sanitizedMovieId, qualityId, latency, speed);
        return new ReportSegmentAckMessage(sanitizedSegmentId);
    }

    /**
     * Handles removal of a segment from a client's cache.
     * Removes the client from the set of peers that own this segment.
     * 
     * @param clientId  the client identifier
     * @param movieId   the movie identifier
     * @param qualityId the quality variant
     * @param segmentId the segment identifier
     */
    public void handleRemoveSegment(
            @NonNull String clientId,
            @NonNull String movieId,
            String qualityId,
            @NonNull String segmentId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);
        String sanitizedSegmentId = Objects.requireNonNull(segmentId, SEGMENT_ID_REQUIRED);

        String segmentKey = StreamingRedisKeys.segmentOwnersKey(sanitizedMovieId, qualityId, sanitizedSegmentId);
        if (segmentKey == null) {
            log.warn(SEGMENT_KEY_NULL, sanitizedMovieId, qualityId, sanitizedSegmentId);
            return;
        }

        Long removed = redisTemplate.opsForSet().remove(segmentKey, sanitizedClientId);
        if (removed != null && removed > 0) {
            log.info("Client {} removed segment {} (movie={}, quality={})",
                    sanitizedClientId, sanitizedSegmentId, sanitizedMovieId, qualityId);
        } else {
            log.debug("Client {} attempted to remove segment {} but was not listed as owner (movie={}, quality={})",
                    sanitizedClientId, sanitizedSegmentId, sanitizedMovieId, qualityId);
        }
    }

    public void handleDisconnect(@NonNull String clientId, @NonNull String movieId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);
        log.info("Client {} disconnected from movie {}", sanitizedClientId, sanitizedMovieId);
        activeClients.remove(sanitizedClientId);
        removeClientFromSegments(sanitizedClientId, sanitizedMovieId);

        String peerKey = StreamingRedisKeys.moviePeersKey(sanitizedMovieId);
        if (peerKey == null) {
            log.warn(PEER_KEY_NULL_FOR_MOVIE, sanitizedMovieId);
            return;
        }
        redisTemplate.opsForSet().remove(peerKey, sanitizedClientId);

        // Clean up peer-specific keys
        cleanupPeerKeys(sanitizedClientId);
    }

    private void cleanupPeerKeys(@NonNull String clientId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);

        // Delete peer metrics key
        String metricsKey = StreamingRedisKeys.peerMetricsKey(sanitizedClientId);
        if (metricsKey != null) {
            Boolean deleted = redisTemplate.delete(metricsKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Deleted metrics key for client {}", sanitizedClientId);
            }
        }

        // Delete peer last seen key
        String lastSeenKey = StreamingRedisKeys.peerLastSeenKey(sanitizedClientId);
        if (lastSeenKey != null) {
            Boolean deleted = redisTemplate.delete(lastSeenKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Deleted last seen key for client {}", sanitizedClientId);
            }
        }

        log.info("Cleaned up Redis keys for disconnected client {}", sanitizedClientId);
    }

    private void removeClientFromSegments(@NonNull String clientId, @NonNull String movieId) {
        String sanitizedClientId = Objects.requireNonNull(clientId, CLIENT_ID_REQUIRED);
        String sanitizedMovieId = Objects.requireNonNull(movieId, MOVIE_ID_REQUIRED);

        // Pattern to match all segments for this movie across all qualities
        // movie:{movieId}:quality:*:segment:*:owners or
        // movie:{movieId}:segment:*:owners
        String pattern = "movie:" + sanitizedMovieId + ":*:segment:*:owners";

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
            log.warn("Failed to scan Redis keys for movie {}: {}", sanitizedMovieId, ex.getMessage());
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
                properties.signaling().peerLastSeenTtl(),
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
