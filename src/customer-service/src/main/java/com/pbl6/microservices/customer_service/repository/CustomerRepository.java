package com.pbl6.microservices.customer_service.repository;

import com.pbl6.microservices.customer_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByAccountId(UUID accountId);
    
    boolean existsByAccountId(UUID accountId);
}
