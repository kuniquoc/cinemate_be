package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.request.WatchProgressRequest;
import com.pbl6.cinemate.movie.dto.response.WatchHistoryDateResponse;
import com.pbl6.cinemate.movie.dto.response.WatchHistoryResponse;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;

import java.time.LocalDate;
import java.util.UUID;
import com.pbl6.cinemate.movie.dto.response.WatchProgressResponse;

public interface WatchHistoryService {

    /**
     * Save or update watch progress for a movie
     */
    void saveWatchProgress(UUID movieId, UUID customerId, WatchProgressRequest request);

    /**
     * Get watch history dates for a customer (paginated)
     */
    PaginatedResponse<WatchHistoryDateResponse> getWatchHistoryDates(UUID customerId, int page, int size);

    /**
     * Get watch history for a customer by date (paginated)
     */
    PaginatedResponse<WatchHistoryResponse> getWatchHistoryByDate(UUID customerId, LocalDate date, int page, int size);

    /**
     * Get last watched position for a movie by customer
     * Returns null if no watch history exists
     */
    Long getLastWatchedPosition(UUID movieId, UUID customerId);

    /**
     * Get watch progress (last position and total duration) for a movie and
     * customer
     */
    WatchProgressResponse getWatchProgress(UUID movieId, UUID customerId);

    /**
     * Delete watch history for a specific movie
     */
    void deleteWatchHistory(UUID movieId, UUID customerId);
}
