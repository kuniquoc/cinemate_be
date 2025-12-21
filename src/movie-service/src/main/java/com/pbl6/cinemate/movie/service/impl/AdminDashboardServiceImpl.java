package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.client.AuthServiceClient;
import com.pbl6.cinemate.movie.client.CustomerServiceClient;
import com.pbl6.cinemate.movie.client.PaymentServiceClient;
import com.pbl6.cinemate.movie.client.dto.PaymentStatsResponse;
import com.pbl6.cinemate.movie.client.dto.UserCountResponse;
import com.pbl6.cinemate.movie.dto.response.ChartDataResponse;
import com.pbl6.cinemate.movie.dto.response.DashboardStatsResponse;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.repository.WatchHistoryRepository;
import com.pbl6.cinemate.movie.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Implementation of AdminDashboardService
 * Aggregates statistics from multiple services via FeignClients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final MovieRepository movieRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final AuthServiceClient authServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    public DashboardStatsResponse getStats() {
        log.info("Fetching dashboard statistics");

        // Get local stats
        long totalMovies = movieRepository.count();

        // Get stats from other services via FeignClients
        UserCountResponse userCount = authServiceClient.getUsersCount();
        PaymentStatsResponse paymentStats = paymentServiceClient.getPaymentStats();

        return DashboardStatsResponse.builder()
                .totalUsers(userCount.count())
                .totalMovies(totalMovies)
                .activeSubscriptions(paymentStats.activeSubscriptions())
                .totalRevenue(paymentStats.totalRevenue())
                .ordersToday(paymentStats.ordersToday())
                .build();
    }

    @Override
    public List<ChartDataResponse> getChartData(Instant startDate, Instant endDate) {
        log.info("Fetching chart data from {} to {}", startDate, endDate);

        // Get watch views grouped by date and category from local database
        List<Object[]> watchData = watchHistoryRepository.countWatchViewsByDateAndCategory(startDate, endDate);

        // Get favorite stats from customer service
        List<Map<String, Object>> favoriteData = customerServiceClient.getFavoriteStats(startDate, endDate);

        // Build a map to aggregate data by (date, category)
        // Key: "date|category", Value: ChartDataResponse
        Map<String, ChartDataResponse> chartDataMap = new LinkedHashMap<>();

        // Process watch data
        for (Object[] row : watchData) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            String category = (String) row[1];
            long viewCount = ((Number) row[2]).longValue();

            String key = date.toString() + "|" + category;
            chartDataMap.computeIfAbsent(key, k -> ChartDataResponse.builder()
                    .date(date)
                    .category(category)
                    .watchingView(0L)
                    .favoriteCount(0L)
                    .build());
            chartDataMap.get(key).setWatchingView(viewCount);
        }

        // Process favorite data - need to map movieId to categories
        Map<String, Map<String, Long>> favoriteDateMovieCount = new HashMap<>();
        for (Map<String, Object> item : favoriteData) {
            String dateStr = item.get("date").toString();
            String movieId = item.get("movieId").toString();
            long count = ((Number) item.get("count")).longValue();

            favoriteDateMovieCount
                    .computeIfAbsent(dateStr, k -> new HashMap<>())
                    .put(movieId, count);
        }

        // Map movieId -> categories and aggregate favorite counts by (date, category)
        for (Map.Entry<String, Map<String, Long>> dateEntry : favoriteDateMovieCount.entrySet()) {
            String dateStr = dateEntry.getKey();
            LocalDate date = LocalDate.parse(dateStr);

            for (Map.Entry<String, Long> movieEntry : dateEntry.getValue().entrySet()) {
                String movieIdStr = movieEntry.getKey();
                long favoriteCount = movieEntry.getValue();

                try {
                    UUID movieId = UUID.fromString(movieIdStr);
                    // Get categories for this movie
                    var movieCategories = movieCategoryRepository.findByMovieIdWithCategory(movieId);

                    for (var mc : movieCategories) {
                        String category = mc.getCategory().getName();
                        String key = date.toString() + "|" + category;

                        chartDataMap.computeIfAbsent(key, k -> ChartDataResponse.builder()
                                .date(date)
                                .category(category)
                                .watchingView(0L)
                                .favoriteCount(0L)
                                .build());

                        ChartDataResponse existing = chartDataMap.get(key);
                        existing.setFavoriteCount(existing.getFavoriteCount() + favoriteCount);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid movieId format: {}", movieIdStr);
                }
            }
        }

        // Sort by date descending, then by category
        List<ChartDataResponse> result = new ArrayList<>(chartDataMap.values());
        result.sort((a, b) -> {
            int dateCompare = b.getDate().compareTo(a.getDate());
            if (dateCompare != 0)
                return dateCompare;
            return a.getCategory().compareTo(b.getCategory());
        });

        return result;
    }
}