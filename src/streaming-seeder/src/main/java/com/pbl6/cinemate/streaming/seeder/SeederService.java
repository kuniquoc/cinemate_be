package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.shared.streaming.StreamingRedisKeys;
import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
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

@Service
public class SeederService {

    private static final Logger log = LoggerFactory.getLogger(SeederService.class);
    private final StringRedisTemplate redisTemplate;
    private final SeederProperties properties;
    private final CacheScanner cacheScanner;
    private final Clock clock;

    public SeederService(
            StringRedisTemplate redisTemplate,
            SeederProperties properties,
            CacheScanner cacheScanner,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.cacheScanner = cacheScanner;
        this.clock = clock;
    }

    public List<CachedSegment> scanCache() {
        return cacheScanner.scan(properties.getCachePath());
    }

    public void syncCacheToRedis(List<CachedSegment> cachedSegments) {
        Map<String, List<CachedSegment>> byStream = cachedSegments
                .stream()
                .filter(segment -> segment.streamId() != null && segment.segmentId() != null)
                .collect(Collectors.groupingBy(CachedSegment::streamId));
        Duration ttl = properties.getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }
        Duration effectiveTtl = ttl;
        byStream.forEach((streamId, segments) -> {
            if (segments.isEmpty()) {
                return;
            }
            String key = StreamingRedisKeys.streamSegmentsDirectoryKey(streamId);
            if (key == null) {
                log.debug("Skipping Redis sync for stream with null key");
                return;
            }
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
        Duration window = properties.getSeederCacheWindow();
        Instant now = Instant.now(clock);
        return cachedSegments
                .stream()
                .filter(segment -> segment.lastModified().isBefore(now.minus(window)))
                .toList();
    }

    public void registerFetchedSegment(CachedSegment segment) {
        if (segment == null) {
            return;
        }
        syncCacheToRedis(List.of(segment));
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
            String key = StreamingRedisKeys.streamSegmentsDirectoryKey(streamId);
            if (key == null) {
                log.debug("Skipping Redis cleanup for stream with null key");
                return;
            }
            Long removedCount = redisTemplate
                    .opsForSet()
                    .remove(key, segmentId);
            log.info(
                    "Removed expired segment {} from stream {} (removed count: {})",
                    segment.segmentId(),
                    segment.streamId(),
                    removedCount);
        });
    }

    public void refreshTtlForStreams(Set<String> streamIds) {
        Duration ttl = properties.getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }
        Duration effectiveTtl = ttl;
        String key;
        for (String streamId : streamIds) {
            if (streamId == null) {
                log.debug("Encountered null streamId when refreshing TTL");
            } else {
                key = StreamingRedisKeys.streamSegmentsDirectoryKey(streamId);
                if (key == null) {
                    log.debug("Encountered null key when refreshing TTL for stream {}", streamId);
                } else {
                    Boolean expiryUpdated = redisTemplate.expire(
                            key,
                            effectiveTtl);
                    if (!Boolean.TRUE.equals(expiryUpdated)) {
                        log.debug("Failed to refresh TTL for stream {}", streamId);
                    }
                }
            }
        }
    }
}
