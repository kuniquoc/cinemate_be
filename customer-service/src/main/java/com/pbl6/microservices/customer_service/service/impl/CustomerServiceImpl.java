package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.constants.ErrorMessage;
import com.pbl6.microservices.customer_service.entity.Customer;
import com.pbl6.microservices.customer_service.enums.Gender;
import com.pbl6.microservices.customer_service.event.kafka.UserRegisteredEvent;
import com.pbl6.microservices.customer_service.exception.NotFoundException;
import com.pbl6.microservices.customer_service.mapper.CustomerMapper;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.CustomerResponse;
import com.pbl6.microservices.customer_service.repository.CustomerRepository;
import com.pbl6.microservices.customer_service.service.CustomerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    @Transactional
    @Override
    public CustomerResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getAvatarUrl() != null) customer.setAvatarUrl(request.getAvatarUrl());
        if (request.getDateOfBirth() != null) customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) customer.setGender(Gender.valueOf(request.getGender()));
        if (request.getDisplayLang() != null) customer.setDisplayLang(request.getDisplayLang());


        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    public void createCustomer(UserRegisteredEvent userRegisteredEvent) {
        Customer customer = Customer.builder()
                .accountId(userRegisteredEvent.getAccountId())
                .firstName(userRegisteredEvent.getFirstName())
                .lastName(userRegisteredEvent.getLastName())
                .build();
        customerRepository.save(customer);
    }
}
