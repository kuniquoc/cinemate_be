package com.pbl6.cinemate.streaming.seeder.web;

import com.pbl6.cinemate.streaming.seeder.CachedSegment;
import com.pbl6.cinemate.streaming.seeder.OriginSegmentFetcher;
import com.pbl6.cinemate.streaming.seeder.SeederService;
import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/streams/{streamId}/segments")
public class SegmentController {

    private static final Logger log = LoggerFactory.getLogger(SegmentController.class);
    private final SeederProperties properties;
    private final OriginSegmentFetcher originSegmentFetcher;
    private final SeederService seederService;

    public SegmentController(SeederProperties properties, OriginSegmentFetcher originSegmentFetcher,
            SeederService seederService) {
        this.properties = Objects.requireNonNull(properties);
        this.originSegmentFetcher = Objects.requireNonNull(originSegmentFetcher);
        this.seederService = Objects.requireNonNull(seederService);
    }

    @GetMapping("/{segmentId}")
    public ResponseEntity<Resource> getSegment(
            @PathVariable String streamId,
            @PathVariable String segmentId) throws IOException {
        if (!isSafeIdentifier(streamId) || !isSafeIdentifier(segmentId)) {
            return ResponseEntity.badRequest().build();
        }
        Path path = locateSegment(streamId, segmentId);
        if (path == null) {
            Optional<CachedSegment> fetched = originSegmentFetcher.fetchFromOrigin(streamId, segmentId);
            if (fetched.isPresent()) {
                CachedSegment segment = fetched.get();
                seederService.registerFetchedSegment(segment);
                path = segment.path();
            }
        }
        if (path == null) {
            log.debug("Segment {} not found for stream {}", segmentId, streamId);
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri(), "Segment URI must not be null"));
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("Segment {} exists but is not readable", path);
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = detectMediaType(path);
        long contentLength = Files.size(path);
        return ResponseEntity
                .ok()
                .contentType(Objects.requireNonNull(mediaType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    private Path locateSegment(String streamId, String segmentId) throws IOException {
        Path streamDir = properties.getCachePath().resolve(streamId);
        if (!Files.isDirectory(streamDir)) {
            return null;
        }
        Path exactPath = streamDir.resolve(segmentId);
        if (Files.exists(exactPath)) {
            return exactPath;
        }
        try (DirectoryStream<Path> candidates = Files.newDirectoryStream(streamDir, segmentId + ".*")) {
            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isSafeIdentifier(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("..")
                && !value.contains("/")
                && !value.contains("\\");
    }

    private MediaType detectMediaType(Path path) {
        try {
            String probe = Files.probeContentType(path);
            if (probe != null) {
                return MediaType.parseMediaType(probe);
            }
        } catch (IOException ex) {
            log.debug("Failed to probe content type for {}: {}", path, ex.getMessage());
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".ts") || filename.endsWith(".m2ts")) {
            return MediaType.valueOf("video/mp2t");
        }
        if (filename.endsWith(".m4s")) {
            return MediaType.valueOf("video/iso.segment");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
