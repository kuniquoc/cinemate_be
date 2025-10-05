package com.pbl6.microservices.customer_service.service;

import com.pbl6.microservices.customer_service.event.kafka.UserRegisteredEvent;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.CustomerResponse;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface CustomerService {
    @Transactional
    CustomerResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void createCustomer(UserRegisteredEvent userRegisteredEvent);
}
