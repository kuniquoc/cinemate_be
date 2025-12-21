package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal API for statistics - only accessible within Docker network
 */
@RestController
@RequestMapping("/api/internal/stats")
@RequiredArgsConstructor
public class InternalStatsController {

    private final InternalStatsService internalStatsService;

    @GetMapping("/users-count")
    public ResponseEntity<Map<String, Long>> getUsersCount() {
        long count = internalStatsService.getUsersCount();
        return ResponseEntity.ok(Map.of("count", count));
    }
}