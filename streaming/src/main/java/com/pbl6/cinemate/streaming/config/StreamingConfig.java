package com.pbl6.cinemate.streaming.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StreamingProperties.class)
public class StreamingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "seederMaintenanceInterval")
    public String seederMaintenanceInterval(StreamingProperties properties) {
        return Long.toString(properties.getSeeder().getCacheMaintenanceInterval().toMillis());
    }
}
