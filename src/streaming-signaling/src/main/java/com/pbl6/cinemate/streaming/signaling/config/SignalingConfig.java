package com.pbl6.cinemate.streaming.signaling.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SignalingProperties.class)
public class SignalingConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
