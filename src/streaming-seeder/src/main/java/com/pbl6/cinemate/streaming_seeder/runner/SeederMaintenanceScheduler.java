package com.pbl6.cinemate.streaming_seeder.runner;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.config.SeederProperties;
import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;
import com.pbl6.cinemate.streaming_seeder.service.SeederService;

@Component
public class SeederMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeederMaintenanceScheduler.class);
    private final SeederProperties properties;
    private final SeederService seederService;

    public SeederMaintenanceScheduler(SeederProperties properties, SeederService seederService) {
        this.properties = properties;
        this.seederService = seederService;
    }

    @Scheduled(fixedDelayString = "#{@seederMaintenanceInterval}")
    public void performMaintenance() {
        if (!properties.enabled()) {
            return;
        }
        List<CachedSegment> segments = seederService.scanCache();
        if (!segments.isEmpty()) {
            seederService.syncCacheToRedis(segments);
        }
        List<CachedSegment> expiredSegments = seederService.findExpiredSegments(segments);
        if (!expiredSegments.isEmpty()) {
            seederService.purgeExpiredSegments(expiredSegments);
        }
        Set<String> activeMovies = segments.stream().map(CachedSegment::movieId).collect(Collectors.toSet());
        if (!activeMovies.isEmpty()) {
            seederService.refreshTtlForMovies(activeMovies);
        }
        log.debug(
                "Seeder maintenance tick complete (segments={}, expired={}, movies={})",
                segments.size(),
                expiredSegments.size(),
                activeMovies.size());
    }
}
