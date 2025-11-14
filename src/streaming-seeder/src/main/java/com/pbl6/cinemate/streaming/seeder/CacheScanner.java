package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.seeder.SegmentKey.SegmentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Scans the local cache directory for fMP4/DASH segments.
 * Expected structure:
 * cache/
 * {movieId}/
 * master.m3u8 (master playlist, optional)
 * {quality}/
 * init.mp4 or init.m4s (init segment)
 * playlist.m3u8 (variant playlist)
 * seg_0001.m4s (media segments)
 * seg_0002.m4s
 */
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
        try (Stream<Path> movieDirs = Files.list(cachePath)) {
            movieDirs
                    .filter(Files::isDirectory)
                    .forEach(movieDir -> scanMovieDirectory(movieDir, segments));
        } catch (IOException ex) {
            log.warn("Failed to scan cache path {}: {}", cachePath, ex.getMessage());
        }
        log.debug("Found {} cached segments under {}", segments.size(), cachePath);
        return segments;
    }

    private void scanMovieDirectory(Path movieDir, List<CachedSegment> segments) {
        String movieId = movieDir.getFileName().toString();

        // Scan for master playlist at movie level
        scanForMasterPlaylist(movieDir, movieId, segments);

        // Scan quality directories
        try (Stream<Path> qualityDirs = Files.list(movieDir)) {
            qualityDirs
                    .filter(Files::isDirectory)
                    .forEach(qualityDir -> scanQualityDirectory(qualityDir, movieId, segments));
        } catch (IOException ex) {
            log.debug("Failed to scan movie directory {}: {}", movieDir, ex.getMessage());
        }
    }

    private void scanForMasterPlaylist(Path movieDir, String movieId, List<CachedSegment> segments) {
        try (Stream<Path> files = Files.list(movieDir)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.equals("master.m3u8") || name.equals("playlist.m3u8");
                    })
                    .forEach(path -> {
                        CachedSegment segment = createSegment(
                                movieId, null, stripExtension(path.getFileName().toString()),
                                path, SegmentType.MASTER_PLAYLIST);
                        if (segment != null) {
                            segments.add(segment);
                            log.debug("Found master playlist: {}", path);
                        }
                    });
        } catch (IOException ex) {
            log.debug("Failed to scan for master playlist in {}: {}", movieDir, ex.getMessage());
        }
    }

    private void scanQualityDirectory(Path qualityDir, String movieId, List<CachedSegment> segments) {
        String qualityId = qualityDir.getFileName().toString();

        try (Stream<Path> files = Files.list(qualityDir)) {
            files
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String segmentId = stripExtension(fileName);
                        SegmentType type = detectSegmentType(fileName);

                        CachedSegment segment = createSegment(movieId, qualityId, segmentId, path, type);
                        if (segment != null) {
                            segments.add(segment);
                        }
                    });
        } catch (IOException ex) {
            log.debug("Failed to scan quality directory {}: {}", qualityDir, ex.getMessage());
        }
    }

    private CachedSegment createSegment(String movieId, String qualityId, String segmentId,
            Path filePath, SegmentType type) {
        try {
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
            return new CachedSegment(movieId, qualityId, segmentId, filePath, lastModified, type);
        } catch (IOException ex) {
            log.debug("Failed to read attributes for {}: {}", filePath, ex.getMessage());
            return new CachedSegment(movieId, qualityId, segmentId, filePath, Instant.now(clock), type);
        }
    }

    private SegmentType detectSegmentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);

        // Init segment
        if (lower.startsWith("init.") || lower.equals("init")) {
            return SegmentType.INIT;
        }

        // Playlist
        if (lower.endsWith(".m3u8")) {
            if (lower.startsWith("master")) {
                return SegmentType.MASTER_PLAYLIST;
            }
            return SegmentType.VARIANT_PLAYLIST;
        }

        // Media segment (default)
        return SegmentType.MEDIA;
    }

    private String stripExtension(String value) {
        int index = value.lastIndexOf('.');
        return index > 0 ? value.substring(0, index) : value;
    }
}
