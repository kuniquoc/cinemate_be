package com.pbl6.cinemate.auth_service.event.kafka.publisher;

import com.pbl6.cinemate.auth_service.event.kafka.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredPublisher {
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent to Kafka topic 'user-registered' at {}", System.currentTimeMillis());
        log.info("event: {}", event);
        kafkaTemplate.send("user-registered", event);
        log.info("User registered successfully: {}", event);
    }
}