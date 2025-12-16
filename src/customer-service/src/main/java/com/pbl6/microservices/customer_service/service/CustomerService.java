package com.pbl6.microservices.customer_service.service;

import com.pbl6.microservices.customer_service.event.kafka.UserRegisteredEvent;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.CustomerInfoResponse;
import com.pbl6.microservices.customer_service.payload.response.CustomerResponse;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface CustomerService {
    CustomerResponse getProfile(UUID accountId);

    @Transactional
    CustomerResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void createCustomer(UserRegisteredEvent userRegisteredEvent);

    /**
     * Get customer info for internal service calls
     * Returns lightweight response with firstName, lastName, avatarUrl
     */
    CustomerInfoResponse getCustomerInfo(UUID accountId);
}
