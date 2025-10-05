package com.pbl6.microservices.customer_service.mapper;

import com.pbl6.microservices.customer_service.entity.Customer;
import com.pbl6.microservices.customer_service.payload.response.CustomerResponse;

public class CustomerMapper {
    public static CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .accountId(customer.getAccountId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .avatarUrl(customer.getAvatarUrl())
                .dateOfBirth(customer.getDateOfBirth())
                .gender(customer.getGender().name())
                .displayLang(customer.getDisplayLang())
                .isAnonymous(customer.getIsAnonymous())
                .build();
    }
}
