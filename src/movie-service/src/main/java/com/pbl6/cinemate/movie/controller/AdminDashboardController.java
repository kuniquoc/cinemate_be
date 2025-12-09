package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.response.ChartDataResponse;
import com.pbl6.cinemate.movie.dto.response.DashboardStatsResponse;
import com.pbl6.cinemate.movie.service.AdminDashboardService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin Dashboard Controller
 * Provides endpoints for admin dashboard statistics and chart data
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin dashboard statistics and analytics")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "Get dashboard statistics", description = "Returns aggregated statistics including total users, movies, active subscriptions, revenue, and today's orders")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ResponseData> getStats(HttpServletRequest httpServletRequest) {
        DashboardStatsResponse stats = adminDashboardService.getStats();

        return ResponseEntity.ok(ResponseData.success(
                stats,
                "Dashboard statistics retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get chart data", description = "Returns chart data grouped by date and category for watching views and favorites")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chart")
    public ResponseEntity<ResponseData> getChartData(
            @Parameter(description = "Start date (inclusive)", example = "2024-01-01") @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", example = "2024-12-31") @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpServletRequest) {

        List<ChartDataResponse> chartData = adminDashboardService.getChartData(startDate, endDate);

        return ResponseEntity.ok(ResponseData.success(
                chartData,
                "Chart data retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
