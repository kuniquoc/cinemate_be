package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.CustomerInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation for CustomerClient
 * Returns null avatarUrl when customer-service is unavailable
 * The userName will still be available from JWT claims (firstName + lastName)
 */
@Component
@Slf4j
public class CustomerClientFallback implements CustomerClient {

    @Override
    public CustomerInfoResponse getCustomerInfo(UUID accountId) {
        log.warn("CustomerClient fallback triggered for accountId: {}. Customer service may be unavailable.",
                accountId);

        // Return empty response - caller should use JWT claims as fallback
        return CustomerInfoResponse.builder()
                .firstName(null)
                .lastName(null)
                .avatarUrl(null)
                .build();
    }
}
