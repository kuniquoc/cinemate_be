package com.pbl6.cinemate.streaming_seeder.service;

import java.nio.file.Path;

/**
 * Locates segments in the local cache file system.
 */
public interface SegmentLocator {

    /**
     * Locates a segment in the cache directory.
     *
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param segmentId the segment identifier
     * @return the path to the segment file, or null if not found
     */
    Path locate(String movieId, String qualityId, String segmentId);
}
