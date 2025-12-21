package com.pbl6.cinemate.streaming_seeder.client;

import com.pbl6.cinemate.streaming_seeder.dto.ContentAccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Payment Service internal content access check
 */
@FeignClient(name = "payment-service", url = "${payment.service.url:http://payment-service:8080}")
public interface PaymentServiceClient {

    @PostMapping("/api/internal/content-access/check")
    ResponseEntity<Map<String, Object>> checkContentAccess(
            @RequestParam("userId") UUID userId,
            @RequestParam("movieCategoryIds") List<UUID> movieCategoryIds,
            @RequestParam("currentWatchTimeMinutes") Integer currentWatchTimeMinutes);
}
