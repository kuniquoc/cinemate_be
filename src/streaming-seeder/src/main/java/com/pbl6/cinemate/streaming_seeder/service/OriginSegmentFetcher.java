package com.pbl6.cinemate.streaming_seeder.service;

import java.util.Optional;

import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;

/**
 * Fetches segments from origin storage (MinIO/S3).
 */
public interface OriginSegmentFetcher {

    /**
     * Fetches a segment from origin storage.
     *
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param segmentId the segment identifier
     * @return Optional containing the cached segment if found
     */
    Optional<CachedSegment> fetchFromOrigin(String movieId, String qualityId, String segmentId);
}
