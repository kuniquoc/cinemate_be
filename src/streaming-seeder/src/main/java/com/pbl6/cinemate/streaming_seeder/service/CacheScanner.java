package com.pbl6.cinemate.streaming_seeder.service;

import java.nio.file.Path;
import java.util.List;

import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;

/**
 * Scans the local cache directory for fMP4/DASH segments.
 */
public interface CacheScanner {

    /**
     * Scans the cache path for cached segments.
     *
     * @param cachePath the root cache directory path
     * @return list of discovered cached segments
     */
    List<CachedSegment> scan(Path cachePath);
}
