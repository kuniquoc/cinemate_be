package com.pbl6.cinemate.streaming_seeder.service;

import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey.SegmentType;

/**
 * Builds MinIO object names (paths) for segments.
 */
public interface MinioObjectNameBuilder {

    /**
     * Builds the MinIO object name (path) for a segment.
     *
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param fileName  the complete filename including extension
     * @param type      the segment type
     * @return the complete MinIO object path
     */
    String buildObjectName(String movieId, String qualityId, String fileName, SegmentType type);
}
