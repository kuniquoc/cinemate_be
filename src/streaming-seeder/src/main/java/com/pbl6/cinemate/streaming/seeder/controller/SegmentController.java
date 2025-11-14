package com.pbl6.cinemate.streaming.seeder.controller;

import com.pbl6.cinemate.streaming.seeder.CachedSegment;
import com.pbl6.cinemate.streaming.seeder.OriginSegmentFetcher;
import com.pbl6.cinemate.streaming.seeder.SeederService;
import com.pbl6.cinemate.streaming.seeder.service.SegmentFileServer;
import com.pbl6.cinemate.streaming.seeder.service.SegmentLocator;
import com.pbl6.cinemate.streaming.seeder.validation.SegmentIdentifierValidator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving streaming segments.
 * Handles HTTP requests for master playlists, variant playlists, init segments,
 * and media segments.
 */
@RestController
@RequestMapping("/api/streams/")
public class SegmentController {

    private static final Logger log = LoggerFactory.getLogger(SegmentController.class);

    private final SegmentIdentifierValidator validator;
    private final SegmentLocator segmentLocator;
    private final SegmentFileServer fileServer;
    private final OriginSegmentFetcher originSegmentFetcher;
    private final SeederService seederService;

    public SegmentController(
            SegmentIdentifierValidator validator,
            SegmentLocator segmentLocator,
            SegmentFileServer fileServer,
            OriginSegmentFetcher originSegmentFetcher,
            SeederService seederService) {
        this.validator = Objects.requireNonNull(validator);
        this.segmentLocator = Objects.requireNonNull(segmentLocator);
        this.fileServer = Objects.requireNonNull(fileServer);
        this.originSegmentFetcher = Objects.requireNonNull(originSegmentFetcher);
        this.seederService = Objects.requireNonNull(seederService);
    }

    @GetMapping("/movies/{movieId}/master.m3u8")
    public ResponseEntity<Resource> getMasterPlaylist(@PathVariable("movieId") String movieId) throws IOException {
        if (!validator.isSafeIdentifier(movieId)) {
            return ResponseEntity.badRequest().build();
        }
        return serveSegment(movieId, null, "master");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/init.{ext}")
    public ResponseEntity<Resource> getInitSegment(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId,
            @PathVariable("ext") String ext) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)) {
            return ResponseEntity.badRequest().build();
        }
        return serveSegment(movieId, qualityId, "init");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/playlist.m3u8")
    public ResponseEntity<Resource> getVariantPlaylist(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)) {
            return ResponseEntity.badRequest().build();
        }
        return serveSegment(movieId, qualityId, "playlist");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/segments/{segmentId}")
    public ResponseEntity<Resource> getSegment(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId,
            @PathVariable("segmentId") String segmentId) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)
                || !validator.isSafeIdentifier(segmentId)) {
            return ResponseEntity.badRequest().build();
        }
        return serveSegment(movieId, qualityId, segmentId);
    }

    /**
     * Serves a segment by locating it in cache or fetching from origin.
     */
    private ResponseEntity<Resource> serveSegment(String movieId, String qualityId, String segmentId)
            throws IOException {
        Path path = segmentLocator.locate(movieId, qualityId, segmentId);

        if (path == null) {
            // Try to fetch from origin
            Optional<CachedSegment> fetched = originSegmentFetcher.fetchFromOrigin(movieId, qualityId, segmentId);
            if (fetched.isPresent()) {
                CachedSegment segment = fetched.get();
                seederService.registerFetchedSegment(segment);
                path = segment.path();
            }
        }

        if (path == null) {
            log.debug("Segment {} not found for movie {} quality {}", segmentId, movieId, qualityId);
            return ResponseEntity.notFound().build();
        }

        return fileServer.serve(path);
    }
}
