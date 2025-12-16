package com.pbl6.cinemate.streaming_seeder.service.impl;

import java.nio.file.Path;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.service.CacheControlStrategy;

@Component
public class CacheControlStrategyImpl implements CacheControlStrategy {

    private static final long ONE_HOUR = 3600;
    private static final long ONE_DAY = 86400;

    @Override
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
