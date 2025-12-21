package com.pbl6.microservices.customer_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service for internal statistics
 */
public interface InternalStatsService {

    /**
     * Get favorite counts grouped by date and movieId
     * 
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return List of {date, movieId, count}
     */
    List<Map<String, Object>> getFavoriteStats(Instant startDate, Instant endDate);
}