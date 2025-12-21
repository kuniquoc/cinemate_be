package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.response.PaymentStatsResponse;
import com.pbl6.cinemate.payment_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API for statistics - only accessible within Docker network
 */
@RestController
@RequestMapping("/api/internal/stats")
@RequiredArgsConstructor
public class InternalStatsController {

    private final InternalStatsService internalStatsService;

    @GetMapping
    public ResponseEntity<PaymentStatsResponse> getPaymentStats() {
        PaymentStatsResponse response = internalStatsService.getPaymentStats();
        return ResponseEntity.ok(response);
    }
}