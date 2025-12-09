package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.response.ChartDataResponse;
import com.pbl6.cinemate.movie.dto.response.DashboardStatsResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for admin dashboard statistics
 */
public interface AdminDashboardService {

    /**
     * Get aggregated dashboard statistics
     * 
     * @return Stats including totalUsers, totalMovies, activeSubscriptions,
     *         totalRevenue, ordersToday
     */
    DashboardStatsResponse getStats();

    /**
     * Get chart data grouped by date and category
     * 
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return List of chart data points with watchingView and favoriteCount per
     *         date/category
     */
    List<ChartDataResponse> getChartData(LocalDate startDate, LocalDate endDate);
}
