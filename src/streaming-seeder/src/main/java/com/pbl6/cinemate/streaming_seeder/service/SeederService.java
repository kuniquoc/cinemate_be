package com.pbl6.cinemate.streaming_seeder.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;

/**
 * Manages cached segments on the seeder node.
 */
public interface SeederService {

    /**
     * Scans the local cache for segments.
     *
     * @return list of cached segments
     */
    List<CachedSegment> scanCache();

    /**
     * Syncs cached segments to Redis.
     *
     * @param cachedSegments the segments to sync
     */
    void syncCacheToRedis(List<CachedSegment> cachedSegments);

    /**
     * Finds expired media segments.
     *
     * @param cachedSegments the segments to check
     * @return list of expired segments
     */
    List<CachedSegment> findExpiredSegments(List<CachedSegment> cachedSegments);

    /**
     * Registers a fetched segment in Redis.
     *
     * @param segment the segment to register
     */
    void registerFetchedSegment(CachedSegment segment);

    /**
     * Purges expired media segments from disk and Redis.
     *
     * @param expiredSegments the segments to purge
     */
    void purgeExpiredSegments(Collection<CachedSegment> expiredSegments);

    /**
     * Refreshes TTL for movie segments in Redis.
     *
     * @param movieIds the movie IDs to refresh
     */
    void refreshTtlForMovies(Set<String> movieIds);
}
