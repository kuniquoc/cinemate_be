package com.pbl6.cinemate.streaming.seeder.config;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streaming.seeder")
public class SeederProperties {

    private boolean enabled = true;

    @NotNull
    private Path cachePath = Paths.get("cache");

    @NotNull
    private Duration redisTtlSegmentKeys = Duration.ofSeconds(90);

    @NotNull
    private Duration cacheMaintenanceInterval = Duration.ofSeconds(30);

    @NotNull
    private Duration seederCacheWindow = Duration.ofMinutes(4);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getCachePath() {
        return cachePath;
    }

    public void setCachePath(Path cachePath) {
        this.cachePath = cachePath;
    }

    public Duration getRedisTtlSegmentKeys() {
        return redisTtlSegmentKeys;
    }

    public void setRedisTtlSegmentKeys(Duration redisTtlSegmentKeys) {
        this.redisTtlSegmentKeys = redisTtlSegmentKeys;
    }

    public Duration getCacheMaintenanceInterval() {
        return cacheMaintenanceInterval;
    }

    public void setCacheMaintenanceInterval(Duration cacheMaintenanceInterval) {
        this.cacheMaintenanceInterval = cacheMaintenanceInterval;
    }

    public Duration getSeederCacheWindow() {
        return seederCacheWindow;
    }

    public void setSeederCacheWindow(Duration seederCacheWindow) {
        this.seederCacheWindow = seederCacheWindow;
    }
}
