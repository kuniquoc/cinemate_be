package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.config.StreamingProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeederStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeederStartupRunner.class);
    private final StreamingProperties properties;
    private final SeederService seederService;

    public SeederStartupRunner(StreamingProperties properties, SeederService seederService) {
        this.properties = properties;
        this.seederService = seederService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getSeeder().isEnabled()) {
            log.info("Seeder startup routine disabled via configuration");
            return;
        }

        log.info("Seeder starting cache discovery at {}", properties.getSeeder().getCachePath());
        List<CachedSegment> segments = seederService.scanCache();
        log.info("Seeder discovered {} segments in local cache", segments.size());
        seederService.syncCacheToRedis(segments);
        log.info("Seeder ready to serve peers");
    }
}
