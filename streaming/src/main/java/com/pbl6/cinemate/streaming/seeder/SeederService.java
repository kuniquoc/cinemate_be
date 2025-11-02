package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.config.StreamingProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class SeederService {

    private static final Logger log = LoggerFactory.getLogger(SeederService.class);
    private static final String REDIS_KEY_NULL_MESSAGE = "Redis key must not be null";
    private final StringRedisTemplate redisTemplate;
    private final StreamingProperties properties;
    private final CacheScanner cacheScanner;
    private final Clock clock;

    public SeederService(
            StringRedisTemplate redisTemplate,
            StreamingProperties properties,
            CacheScanner cacheScanner,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.cacheScanner = cacheScanner;
        this.clock = clock;
    }

    public List<CachedSegment> scanCache() {
        return cacheScanner.scan(properties.getSeeder().getCachePath());
    }

    public void syncCacheToRedis(List<CachedSegment> cachedSegments) {
        Map<String, List<CachedSegment>> byStream = cachedSegments
                .stream()
                .filter(segment -> segment.streamId() != null && segment.segmentId() != null)
                .collect(Collectors.groupingBy(CachedSegment::streamId));
        Duration ttl = properties.getSeeder().getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }
        Duration effectiveTtl = ttl;
        byStream.forEach((streamId, segments) -> {
            if (segments.isEmpty()) {
                return;
            }
            String key = Objects.requireNonNull(segmentsKey(streamId), REDIS_KEY_NULL_MESSAGE);
            long addedCount = 0L;
            for (CachedSegment segment : segments) {
                String segmentId = Objects.requireNonNull(segment.segmentId(), "segmentId must not be null");
                Long added = redisTemplate.opsForSet().add(key, segmentId);
                if (added != null) {
                    addedCount += added;
                }
            }
            Boolean expiryUpdated = redisTemplate.expire(key, effectiveTtl);
            if (!Boolean.TRUE.equals(expiryUpdated)) {
                log.debug("Failed to refresh TTL for stream {}", streamId);
            }
            log.info(
                    "Registered {} segments in Redis for stream {} (expire updated: {})",
                    addedCount,
                    streamId,
                    Boolean.TRUE.equals(expiryUpdated));
        });
    }

    public List<CachedSegment> findExpiredSegments(List<CachedSegment> cachedSegments) {
        Duration window = properties.getSeeder().getSeederCacheWindow();
        Instant now = Instant.now(clock);
        return cachedSegments
                .stream()
                .filter(segment -> segment.lastModified().isBefore(now.minus(window)))
                .toList();
    }

    public void purgeExpiredSegments(Collection<CachedSegment> expiredSegments) {
        expiredSegments.forEach(segment -> {
            Path path = segment.path();
            try {
                boolean deleted = Files.deleteIfExists(path);
                if (!deleted) {
                    log.debug("File {} did not exist when attempting to delete", path);
                }
            } catch (IOException ex) {
                log.debug("Failed to delete file {}: {}", path, ex.getMessage());
            }
            String streamId = segment.streamId();
            String segmentId = segment.segmentId();
            if (streamId == null || segmentId == null) {
                log.debug("Skipping Redis cleanup for segment with missing identifiers");
                return;
            }
            Long removedCount = redisTemplate
                    .opsForSet()
                    .remove(Objects.requireNonNull(segmentsKey(streamId), REDIS_KEY_NULL_MESSAGE), segmentId);
            log.info(
                    "Removed expired segment {} from stream {} (removed count: {})",
                    segment.segmentId(),
                    segment.streamId(),
                    removedCount);
        });
    }

    public void refreshTtlForStreams(Set<String> streamIds) {
        Duration ttl = properties.getSeeder().getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }
        Duration effectiveTtl = ttl;
        for (String streamId : streamIds) {
            if (streamId == null) {
                log.debug("Encountered null streamId when refreshing TTL");
                continue;
            }
            Boolean expiryUpdated = redisTemplate.expire(
                    Objects.requireNonNull(segmentsKey(streamId), REDIS_KEY_NULL_MESSAGE),
                    effectiveTtl);
            if (!Boolean.TRUE.equals(expiryUpdated)) {
                log.debug("Failed to refresh TTL for stream {}", streamId);
            }
        }
    }

    private @NonNull String segmentsKey(String streamId) {
        return "stream:" + streamId + ":segments";
    }
}
