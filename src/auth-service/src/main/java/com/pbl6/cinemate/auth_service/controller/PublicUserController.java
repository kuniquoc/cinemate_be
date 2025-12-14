package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.payload.response.UserEmailResponse;
import com.pbl6.cinemate.auth_service.service.AccountService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/users")
@RequiredArgsConstructor
public class PublicUserController {
    
    private final AccountService accountService;
    
    @GetMapping("/{userId}/email")
    public ResponseEntity<ResponseData> getEmailByUserId(
            @PathVariable UUID userId,
            HttpServletRequest httpServletRequest) {
        UserEmailResponse response = accountService.getEmailByUserId(userId);
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Email retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
