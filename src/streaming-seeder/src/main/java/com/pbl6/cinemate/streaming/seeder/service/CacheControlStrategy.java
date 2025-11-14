package com.pbl6.cinemate.streaming.seeder.service;

import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Determines cache control headers for streaming segments.
 */
@Component
public class CacheControlStrategy {

    private static final long ONE_HOUR = 3600;
    private static final long ONE_DAY = 86400;

    /**
     * Gets the appropriate cache control header for a file.
     */
    public String getCacheControl(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);

        // Init segments and playlists can be cached longer
        if (filename.startsWith("init.") || filename.endsWith(".m3u8")) {
            return "public, max-age=" + ONE_HOUR; // 1 hour
        }

        // Media segments can be cached
        return "public, max-age=" + ONE_DAY; // 24 hours
    }
}
