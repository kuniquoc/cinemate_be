package com.pbl6.cinemate.movie.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback implementation for CustomerServiceClient
 */
@Slf4j
@Component
public class CustomerServiceClientFallback implements CustomerServiceClient {

    @Override
    public List<Map<String, Object>> getFavoriteStats(LocalDate startDate, LocalDate endDate) {
        log.warn("Fallback: Failed to get favorite stats from customer-service");
        return Collections.emptyList();
    }
}
