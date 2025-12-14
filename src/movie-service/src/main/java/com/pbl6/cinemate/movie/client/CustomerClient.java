package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.CustomerInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for Customer Service internal API
 * Used to fetch customer information for reviews, comments, etc.
 * <p>
 * Note: This uses internal Docker network URL, not exposed through Gateway
 */
@FeignClient(name = "customer-service", url = "${customer.service.url:http://customer-service:8080}", fallback = CustomerClientFallback.class)
public interface CustomerClient {

    /**
     * Get customer display info by account ID
     *
     * @param accountId the account ID (from JWT/auth-service)
     * @return CustomerInfoResponse with firstName, lastName, avatarUrl
     */
    @GetMapping("/internal/customers/{accountId}")
    CustomerInfoResponse getCustomerInfo(@PathVariable("accountId") UUID accountId);
}
