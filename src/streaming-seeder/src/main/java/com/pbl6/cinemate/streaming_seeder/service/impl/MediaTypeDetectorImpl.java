package com.pbl6.cinemate.streaming_seeder.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.service.MediaTypeDetector;

@Component
public class MediaTypeDetectorImpl implements MediaTypeDetector {

    private static final Logger log = LoggerFactory.getLogger(MediaTypeDetectorImpl.class);

    @Override
    public MediaType detect(Path path) {
        // Try system probe first
        try {
            String probe = Files.probeContentType(path);
            if (probe != null) {
                return MediaType.parseMediaType(probe);
            }
        } catch (IOException ex) {
            log.debug("Failed to probe content type for {}: {}", path, ex.getMessage());
        }

        // Fall back to extension-based detection
        return detectByExtension(path);
    }

    /**
     * Detects media type based on file extension.
     */
    private MediaType detectByExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);

        // fMP4 formats
        if (filename.endsWith(".m4s")) {
            return MediaType.valueOf("video/iso.segment");
        }
        if (filename.endsWith(".mp4")) {
            return MediaType.valueOf("video/mp4");
        }

        // HLS playlists
        if (filename.endsWith(".m3u8")) {
            return MediaType.valueOf("application/vnd.apple.mpegurl");
        }

        // Legacy TS
        if (filename.endsWith(".ts") || filename.endsWith(".m2ts")) {
            return MediaType.valueOf("video/mp2t");
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
