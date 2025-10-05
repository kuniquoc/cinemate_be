package com.pbl6.cinemate.auth_service.event.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.auth_service.event.kafka.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(event); // serialize thành JSON thuần
            kafkaTemplate.send("user-registered", json);
            log.info("Published JSON event: {}", json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserRegisteredEvent", e);
        }
    }
}