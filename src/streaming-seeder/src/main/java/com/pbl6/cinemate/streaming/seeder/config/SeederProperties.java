package com.pbl6.cinemate.streaming.seeder.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    @Valid
    @NotNull
    private OriginProperties origin = new OriginProperties();

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

    public OriginProperties getOrigin() {
        return origin;
    }

    public void setOrigin(OriginProperties origin) {
        this.origin = origin;
    }

    public static class OriginProperties {

        private boolean enabled = true;

        private String bucket;

        @NotNull
        private String objectPrefix = "movies";

        @NotNull
        private List<String> segmentExtensions = new ArrayList<>(List.of("ts", "m4s"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getObjectPrefix() {
            return objectPrefix;
        }

        public void setObjectPrefix(String objectPrefix) {
            this.objectPrefix = objectPrefix;
        }

        public List<String> getSegmentExtensions() {
            return segmentExtensions;
        }

        public void setSegmentExtensions(List<String> segmentExtensions) {
            if (segmentExtensions == null || segmentExtensions.isEmpty()) {
                this.segmentExtensions = new ArrayList<>(List.of("ts", "m4s"));
            } else {
                this.segmentExtensions = new ArrayList<>(segmentExtensions);
            }
        }
    }
}
