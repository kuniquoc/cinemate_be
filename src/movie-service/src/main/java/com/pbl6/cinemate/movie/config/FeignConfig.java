package com.pbl6.cinemate.movie.config;

import com.pbl6.cinemate.movie.client.InteractionRecommenderClientFallback;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for external service calls
 */
@Configuration
public class FeignConfig {

    /**
     * Configure Feign logging level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Configure connection and read timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS, // Connect timeout
                10, TimeUnit.SECONDS, // Read timeout
                true // Follow redirects
        );
    }

    /**
     * Configure retry behavior for failed requests
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
                100, // Initial interval (ms)
                1000, // Max interval (ms)
                3 // Max attempts
        );
    }

    /**
     * Fallback factory for interaction recommender client
     */
    @Bean
    public InteractionRecommenderClientFallback interactionRecommenderClientFallback() {
        return new InteractionRecommenderClientFallback();
    }
}
