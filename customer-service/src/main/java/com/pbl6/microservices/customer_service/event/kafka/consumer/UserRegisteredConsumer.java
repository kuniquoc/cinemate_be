package com.pbl6.microservices.customer_service.event.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.microservices.customer_service.event.kafka.UserRegisteredEvent;
import com.pbl6.microservices.customer_service.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredConsumer {
    private final CustomerService customerService;

    @KafkaListener(topics = "user-registered", groupId = "customer-service")
    public void consume(String message) {
        try {
            log.info("message nhan duoc la: {}", message);
            ObjectMapper mapper = new ObjectMapper();
            UserRegisteredEvent event = mapper.readValue(message, UserRegisteredEvent.class);
            System.out.println("✅ Received UserRegisteredEvent: " + event);
            log.info("Processing UserRegisteredEvent for accountId: {} at time {}", event.getAccountId(), System.currentTimeMillis());
            log.info("UserRegisteredEvent details: {}", event);

            customerService.createCustomer(event);
            log.info("Created successfully customer for accountId: {} at time {}", event.getAccountId(), System.currentTimeMillis());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse UserRegisteredEvent: {}", e.getMessage(), e);
            
        }
    }
}