package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.PaymentStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for Payment Service internal stats API
 */
@FeignClient(name = "payment-service", url = "${payment.service.url:http://payment-service:8080}", fallback = PaymentServiceClientFallback.class)
public interface PaymentServiceClient {

    @GetMapping("/api/internal/stats")
    PaymentStatsResponse getPaymentStats();
}