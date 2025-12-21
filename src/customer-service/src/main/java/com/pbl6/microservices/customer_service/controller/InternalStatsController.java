package com.pbl6.microservices.customer_service.controller;

import com.pbl6.microservices.customer_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Internal API for statistics - only accessible within Docker network
 */
@RestController
@RequestMapping("/api/internal/stats")
@RequiredArgsConstructor
public class InternalStatsController {

    private final InternalStatsService internalStatsService;

    @GetMapping("/favorites")
    public ResponseEntity<List<Map<String, Object>>> getFavoriteStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,

            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {
        List<Map<String, Object>> response = internalStatsService.getFavoriteStats(startDate, endDate);

        return ResponseEntity.ok(response);
    }

}