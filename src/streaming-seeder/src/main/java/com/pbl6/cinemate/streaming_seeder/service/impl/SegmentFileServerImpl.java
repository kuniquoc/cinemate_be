package com.pbl6.cinemate.streaming_seeder.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.service.CacheControlStrategy;
import com.pbl6.cinemate.streaming_seeder.service.MediaTypeDetector;
import com.pbl6.cinemate.streaming_seeder.service.SegmentFileServer;

@Component
public class SegmentFileServerImpl implements SegmentFileServer {

    private static final Logger log = LoggerFactory.getLogger(SegmentFileServerImpl.class);

    private final MediaTypeDetector mediaTypeDetector;
    private final CacheControlStrategy cacheControlStrategy;

    public SegmentFileServerImpl(MediaTypeDetector mediaTypeDetector, CacheControlStrategy cacheControlStrategy) {
        this.mediaTypeDetector = mediaTypeDetector;
        this.cacheControlStrategy = cacheControlStrategy;
    }

    @Override
    public ResponseEntity<Resource> serve(Path path) throws IOException {
        Resource resource = new UrlResource(Objects.requireNonNull(path.toUri(), "Segment URI must not be null"));

        if (!resource.exists() || !resource.isReadable()) {
            log.warn("Segment {} exists but is not readable", path);
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = mediaTypeDetector.detect(path);
        long contentLength = Files.size(path);
        String cacheControl = cacheControlStrategy.getCacheControl(path);

        return ResponseEntity
                .ok()
                .contentType(Objects.requireNonNull(mediaType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .body(resource);
    }
}
