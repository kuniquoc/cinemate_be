package com.pbl6.microservices.customer_service.controller;

import com.pbl6.microservices.customer_service.payload.response.CustomerInfoResponse;
import com.pbl6.microservices.customer_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal API controller for inter-service communication
 * These endpoints are NOT exposed through the API Gateway
 * They are only accessible within the Docker network
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    /**
     * Get customer info by account ID
     * Used by movie-service to get user display info for reviews
     * 
     * @param accountId the account ID (from auth-service)
     * @return CustomerInfoResponse with firstName, lastName, avatarUrl
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<CustomerInfoResponse> getCustomerInfo(@PathVariable UUID accountId) {
        CustomerInfoResponse response = customerService.getCustomerInfo(accountId);
        return ResponseEntity.ok(response);
    }
}
