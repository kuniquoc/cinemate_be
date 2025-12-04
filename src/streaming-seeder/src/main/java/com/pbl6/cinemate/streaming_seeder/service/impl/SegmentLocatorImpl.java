package com.pbl6.cinemate.streaming_seeder.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.config.SeederProperties;
import com.pbl6.cinemate.streaming_seeder.service.SegmentIdNormalizer;
import com.pbl6.cinemate.streaming_seeder.service.SegmentLocator;

@Component
public class SegmentLocatorImpl implements SegmentLocator {

    private final SeederProperties properties;
    private final SegmentIdNormalizer normalizer;

    public SegmentLocatorImpl(SeederProperties properties, SegmentIdNormalizer normalizer) {
        this.properties = properties;
        this.normalizer = normalizer;
    }

    @Override
    public Path locate(String movieId, String qualityId, String segmentId) {
        Path baseDir = properties.cachePath().resolve(movieId);

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
