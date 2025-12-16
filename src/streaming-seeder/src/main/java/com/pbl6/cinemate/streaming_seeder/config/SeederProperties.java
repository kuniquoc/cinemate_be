package com.pbl6.cinemate.streaming_seeder.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "streaming.seeder")
public record SeederProperties(
        boolean enabled,
        @NotNull Path cachePath,
        @NotNull Duration redisTtlSegmentKeys,
        @NotNull Duration cacheMaintenanceInterval,
        @NotNull Duration seederCacheWindow,
        @Valid @NotNull OriginProperties origin) {

    public SeederProperties {
        // Defaults
        if (cachePath == null)
            cachePath = Path.of("cache");
        if (redisTtlSegmentKeys == null)
            redisTtlSegmentKeys = Duration.ofSeconds(90);
        if (cacheMaintenanceInterval == null)
            cacheMaintenanceInterval = Duration.ofSeconds(30);
        if (seederCacheWindow == null)
            seederCacheWindow = Duration.ofMinutes(4);
    }

    public record OriginProperties(
            boolean enabled,
            String bucket,
            @NotNull String objectPrefix,
            @NotNull List<String> segmentExtensions) {
        public OriginProperties {
            if (objectPrefix == null)
                objectPrefix = "movies";
            if (segmentExtensions == null || segmentExtensions.isEmpty()) {
                segmentExtensions = List.of("ts", "m4s");
            }
        }
    }
}
