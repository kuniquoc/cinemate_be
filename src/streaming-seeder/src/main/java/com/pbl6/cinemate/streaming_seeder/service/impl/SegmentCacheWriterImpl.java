package com.pbl6.cinemate.streaming_seeder.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.config.SeederProperties;
import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;
import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey.SegmentType;
import com.pbl6.cinemate.streaming_seeder.service.SegmentCacheWriter;

@Component
public class SegmentCacheWriterImpl implements SegmentCacheWriter {

    private static final Logger log = LoggerFactory.getLogger(SegmentCacheWriterImpl.class);

    private final SeederProperties properties;
    private final Clock clock;

    public SegmentCacheWriterImpl(SeederProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public CachedSegment saveToCache(
            String movieId,
            String qualityId,
            String segmentId,
            String fileName,
            SegmentType type,
            InputStream inputStream) throws IOException {

        Path cacheDir = prepareCacheDirectory(movieId, qualityId, type);
        Path target = cacheDir.resolve(fileName);

        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);

        Instant now = Instant.now(clock);
        Files.setLastModifiedTime(target, FileTime.from(now));

        log.info("Saved {} segment '{}' for movie '{}' quality '{}' to cache at '{}'",
                type, segmentId, movieId, qualityId, target);

        return new CachedSegment(movieId, qualityId, segmentId, target, now, type);
    }

    /**
     * Prepares the cache directory for storing segments.
     */
    private Path prepareCacheDirectory(String movieId, String qualityId, SegmentType type) throws IOException {
        Path baseDir = properties.cachePath().resolve(movieId);
        Path targetDir = determineTargetDirectory(baseDir, qualityId, type);

        Files.createDirectories(targetDir);
        return targetDir;
    }

    /**
     * Determines the target directory based on segment type.
     */
    private Path determineTargetDirectory(Path baseDir, String qualityId, SegmentType type) {
        // Master playlist goes in movie root directory
        if (type == SegmentType.MASTER_PLAYLIST) {
            return baseDir;
        }

        if (qualityId != null && !qualityId.isBlank()) {
            return baseDir.resolve(qualityId);
        }

        throw new IllegalArgumentException("Quality ID is required for non-master segment type: " + type);
    }
}
