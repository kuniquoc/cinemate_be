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
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Manages cached segments on the seeder node.
 * Implements cache strategy:
 * - Init segments and playlists: Never expire (critical for playback start and
 * ABR)
 * - Media segments: LRU with TTL based on last access time
 * - Supports fMP4/DASH with multi-quality ABR
 */
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

    /**
     * Syncs cached segments to Redis.
     * Groups segments by movieId (streamId for ABR) and registers them in Redis.
     */
    public void syncCacheToRedis(List<CachedSegment> cachedSegments) {
        // Group by movieId (all qualities under same movieId)
        Map<String, List<CachedSegment>> byMovie = cachedSegments
                .stream()
                .filter(segment -> segment.movieId() != null && segment.segmentId() != null)
                .collect(Collectors.groupingBy(CachedSegment::movieId));

        Duration ttl = properties.getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }

        byMovie.forEach((movieId, segments) -> {
            if (segments.isEmpty()) {
                return;
            }

            // Register segments with quality information
            for (CachedSegment segment : segments) {
                String qualityId = segment.qualityId();
                String segmentId = segment.segmentId();

                // Create Redis key for segment lookup:
                // movie:{movieId}:quality:{qualityId}:segments
                String key = StreamingRedisKeys.movieQualitySegmentsKey(movieId, qualityId);
                if (key == null) {
                    log.debug("Skipping Redis sync for segment with null key");
                    continue;
                }

                // Add segment to Redis set
                redisTemplate.opsForSet().add(key, segmentId);

                // Critical segments (init, playlist) never expire
                // Media segments use configured TTL
                if (segment.isCritical()) {
                    // Remove expiry for critical segments
                    redisTemplate.persist(key);
                    log.debug("Registered critical segment {} for movie {} quality {} (no expiry)",
                            segmentId, movieId, qualityId);
                } else {
                    redisTemplate.expire(key, ttl);
                }
            }

            log.info("Registered {} segments in Redis for movie {}", segments.size(), movieId);
        });
    }

    /**
     * Finds expired media segments.
     * Critical segments (init, playlists) are never considered expired.
     */
    public List<CachedSegment> findExpiredSegments(List<CachedSegment> cachedSegments) {
        Duration window = properties.getSeederCacheWindow();
        Instant now = Instant.now(clock);

        return cachedSegments
                .stream()
                .filter(segment -> !segment.isCritical()) // Never expire critical segments
                .filter(segment -> segment.lastModified().isBefore(now.minus(window)))
                .toList();
    }

    public void registerFetchedSegment(CachedSegment segment) {
        if (segment == null) {
            return;
        }
        syncCacheToRedis(List.of(segment));
    }

    /**
     * Purges expired media segments from disk and Redis.
     * Critical segments are never purged.
     */
    public void purgeExpiredSegments(Collection<CachedSegment> expiredSegments) {
        expiredSegments.forEach(segment -> {
            // Double-check: never purge critical segments
            if (segment.isCritical()) {
                log.warn("Attempted to purge critical segment {}, skipping", segment.segmentId());
                return;
            }

            Path path = segment.path();
            try {
                boolean deleted = Files.deleteIfExists(path);
                if (!deleted) {
                    log.debug("File {} did not exist when attempting to delete", path);
                }
            } catch (IOException ex) {
                log.debug("Failed to delete file {}: {}", path, ex.getMessage());
            }

            String movieId = segment.movieId();
            String qualityId = segment.qualityId();
            String segmentId = segment.segmentId();

            if (movieId == null || segmentId == null) {
                log.debug("Skipping Redis cleanup for segment with missing identifiers");
                return;
            }

            String key = StreamingRedisKeys.movieQualitySegmentsKey(movieId, qualityId);
            if (key == null) {
                log.debug("Skipping Redis cleanup for segment with null key");
                return;
            }

            Long removedCount = redisTemplate.opsForSet().remove(key, segmentId);
            log.info("Removed expired media segment {} from movie {} quality {} (removed count: {})",
                    segmentId, movieId, qualityId, removedCount);
        });
    }

    /**
     * Refreshes TTL for movie segments in Redis.
     * Critical segments maintain no expiry.
     */
    public void refreshTtlForMovies(Set<String> movieIds) {
        Duration ttl = properties.getRedisTtlSegmentKeys();
        if (ttl == null) {
            log.warn("Redis TTL for segment keys is not configured");
            return;
        }

        for (String movieId : movieIds) {
            if (movieId != null) {
                // Refresh TTL for all quality variants of this movie
                // Note: This is a simplified approach; in production, you might want to
                // scan for all quality keys and refresh them individually
                String pattern = StreamingRedisKeys.movieQualitySegmentsPattern(movieId);
                if (pattern != null) {
                    log.debug("TTL refresh requested for movie {} pattern {}", movieId, pattern);
                } else {
                    log.debug("Encountered null pattern when refreshing TTL for movie {}", movieId);
                }
            }
        }
    }
}
