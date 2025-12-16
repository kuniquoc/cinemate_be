package com.pbl6.cinemate.streaming_seeder.service;

import java.io.IOException;
import java.io.InputStream;

import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;
import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey.SegmentType;

/**
 * Writes segments to the local cache.
 */
public interface SegmentCacheWriter {

    /**
     * Saves a segment from an input stream to the cache.
     *
     * @param movieId     the movie identifier
     * @param qualityId   the quality variant
     * @param segmentId   the segment identifier
     * @param fileName    the file name to save as
     * @param type        the segment type
     * @param inputStream the input stream containing segment data
     * @return the cached segment
     * @throws IOException if an I/O error occurs
     */
    CachedSegment saveToCache(
            String movieId,
            String qualityId,
            String segmentId,
            String fileName,
            SegmentType type,
            InputStream inputStream) throws IOException;
}
