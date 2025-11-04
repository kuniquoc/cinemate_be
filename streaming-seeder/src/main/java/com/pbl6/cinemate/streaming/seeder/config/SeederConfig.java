package com.pbl6.cinemate.streaming.seeder.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SeederProperties.class)
public class SeederConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "seederMaintenanceInterval")
    public String seederMaintenanceInterval(SeederProperties properties) {
        return Long.toString(properties.getCacheMaintenanceInterval().toMillis());
    }
}
