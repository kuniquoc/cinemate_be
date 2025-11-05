package com.pbl6.cinemate.streaming.seeder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CacheScanner {

    private static final Logger log = LoggerFactory.getLogger(CacheScanner.class);
    private final Clock clock;

    public CacheScanner(Clock clock) {
        this.clock = clock;
    }

    public List<CachedSegment> scan(Path cachePath) {
        if (cachePath == null) {
            return List.of();
        }
        if (!Files.exists(cachePath)) {
            log.debug("Cache path {} does not exist yet", cachePath);
            return List.of();
        }

        List<CachedSegment> segments = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(cachePath, 2)) {
            paths
                    .filter(Files::isRegularFile)
                    .map(path -> toCachedSegment(cachePath, path))
                    .filter(Objects::nonNull)
                    .forEach(segments::add);
        } catch (IOException ex) {
            log.warn("Failed to scan cache path {}: {}", cachePath, ex.getMessage());
        }
        log.debug("Found {} cached segments under {}", segments.size(), cachePath);
        return segments;
    }

    private CachedSegment toCachedSegment(Path basePath, Path filePath) {
        Path relative = basePath.relativize(filePath);
        if (relative.getNameCount() < 2) {
            return null;
        }
        String streamId = relative.getName(0).toString();
        String fileName = relative.getFileName().toString();
        String segmentId = stripExtension(fileName);
        try {
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
            return new CachedSegment(streamId, segmentId, filePath, lastModified);
        } catch (IOException ex) {
            log.debug("Failed to read attributes for {}: {}", filePath, ex.getMessage());
            return new CachedSegment(streamId, segmentId, filePath, Instant.now(clock));
        }
    }

    private String stripExtension(String value) {
        int index = value.lastIndexOf('.');
        return index > 0 ? value.substring(0, index) : value;
    }
}
