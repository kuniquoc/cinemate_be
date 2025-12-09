package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.UserCountResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for AuthServiceClient
 */
@Slf4j
@Component
public class AuthServiceClientFallback implements AuthServiceClient {

    @Override
    public UserCountResponse getUsersCount() {
        log.warn("Fallback: Failed to get users count from auth-service");
        return new UserCountResponse(0L);
    }
}
