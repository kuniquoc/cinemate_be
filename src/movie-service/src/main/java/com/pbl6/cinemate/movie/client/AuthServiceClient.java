package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.UserCountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign client for Auth Service internal stats API
 */
@FeignClient(name = "auth-service", url = "${auth.service.url:http://auth-service:8080}", fallback = AuthServiceClientFallback.class)
public interface AuthServiceClient {

    @GetMapping("/api/internal/stats/users-count")
    UserCountResponse getUsersCount();
}
