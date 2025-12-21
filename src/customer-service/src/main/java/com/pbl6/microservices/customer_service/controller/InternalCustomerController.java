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

@RestController
@RequestMapping("/api/internal/customers")
@RequiredArgsConstructor
public class InternalCustomerController {

    private final CustomerService customerService;

    @GetMapping("/{accountId}")
    public ResponseEntity<CustomerInfoResponse> getCustomerInfo(@PathVariable UUID accountId) {
        CustomerInfoResponse response = customerService.getCustomerInfo(accountId);
        return ResponseEntity.ok(response);
    }
}
