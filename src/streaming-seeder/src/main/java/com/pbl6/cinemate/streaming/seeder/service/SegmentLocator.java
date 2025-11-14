package com.pbl6.cinemate.streaming.seeder.service;

import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Locates segments in the local cache file system.
 * SegmentId now includes the file extension (e.g., "seg_0005.m4s"),
 * so we can directly resolve the file path.
 */
@Component
public class SegmentLocator {

    private final SeederProperties properties;
    private final SegmentIdNormalizer normalizer;

    public SegmentLocator(SeederProperties properties, SegmentIdNormalizer normalizer) {
        this.properties = properties;
        this.normalizer = normalizer;
    }

    /**
     * Locates a segment in the cache directory.
     * 
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param segmentId the segment identifier (complete filename with extension)
     * @return the path to the segment file, or null if not found
     */
    public Path locate(String movieId, String qualityId, String segmentId) {
        Path baseDir = properties.getCachePath().resolve(movieId);

        // Sanitize the segmentId (trim whitespace)
        String sanitizedSegmentId = normalizer.sanitize(segmentId);
        if (sanitizedSegmentId == null) {
            return null;
        }

        // Master playlist is at movie level
        if (qualityId == null || qualityId.isBlank()) {
            if (!Files.isDirectory(baseDir)) {
                return null;
            }
            Path filePath = baseDir.resolve(sanitizedSegmentId);
            return Files.isRegularFile(filePath) ? filePath : null;
        }

        // Quality-specific segments
        Path qualityDir = baseDir.resolve(qualityId);
        if (!Files.isDirectory(qualityDir)) {
            return null;
        }

        // SegmentId is now the complete filename, resolve it directly
        Path filePath = qualityDir.resolve(sanitizedSegmentId);
        return Files.isRegularFile(filePath) ? filePath : null;
    }
}
