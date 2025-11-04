package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        if (!properties.isEnabled()) {
            return;
        }
        List<CachedSegment> segments = seederService.scanCache();
        List<CachedSegment> expiredSegments = seederService.findExpiredSegments(segments);
        if (!expiredSegments.isEmpty()) {
            seederService.purgeExpiredSegments(expiredSegments);
        }
        Set<String> activeStreams = segments.stream().map(CachedSegment::streamId).collect(Collectors.toSet());
        if (!activeStreams.isEmpty()) {
            seederService.refreshTtlForStreams(activeStreams);
        }
        log.debug(
                "Seeder maintenance tick complete (segments={}, expired={}, streams={})",
                segments.size(),
                expiredSegments.size(),
                activeStreams.size());
    }
}
