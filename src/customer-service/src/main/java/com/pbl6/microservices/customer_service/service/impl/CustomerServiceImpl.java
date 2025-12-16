package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.constants.ErrorMessage;
import com.pbl6.microservices.customer_service.entity.Customer;
import com.pbl6.microservices.customer_service.enums.Gender;
import com.pbl6.microservices.customer_service.event.kafka.UserRegisteredEvent;
import com.pbl6.microservices.customer_service.exception.NotFoundException;
import com.pbl6.microservices.customer_service.mapper.CustomerMapper;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.CustomerInfoResponse;
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

    @Override
    public CustomerResponse getProfile(UUID accountId) {
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
        return CustomerMapper.toResponse(customer);
    }

    @Transactional
    @Override
    public CustomerResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));

        if (request.getFirstName() != null)
            customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            customer.setLastName(request.getLastName());
        if (request.getAvatarUrl() != null)
            customer.setAvatarUrl(request.getAvatarUrl());
        if (request.getDateOfBirth() != null)
            customer.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            customer.setGender(Gender.valueOf(request.getGender()));
        if (request.getDisplayLang() != null)
            customer.setDisplayLang(request.getDisplayLang());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    public void createCustomer(UserRegisteredEvent userRegisteredEvent) {
        Customer customer = new Customer();
        customer.setAccountId(userRegisteredEvent.getAccountId());

        customer.setFirstName(userRegisteredEvent.getFirstName());
        customer.setLastName(userRegisteredEvent.getLastName());

        customerRepository.save(customer);
    }

    @Override
    public CustomerInfoResponse getCustomerInfo(UUID accountId) {
        Customer customer = customerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.USER_NOT_FOUND));
        return CustomerInfoResponse.builder()
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .avatarUrl(customer.getAvatarUrl())
                .build();
    }
}
