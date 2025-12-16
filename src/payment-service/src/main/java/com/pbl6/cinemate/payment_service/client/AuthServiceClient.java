package com.pbl6.cinemate.payment_service.client;

import com.pbl6.cinemate.shared.dto.general.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "auth-service", url = "${auth.service.url:http://auth-service:8585}")
public interface AuthServiceClient {
    
    @GetMapping("/api/public/users/{userId}/email")
    ResponseData getEmailByUserId(@PathVariable("userId") UUID userId);
}
