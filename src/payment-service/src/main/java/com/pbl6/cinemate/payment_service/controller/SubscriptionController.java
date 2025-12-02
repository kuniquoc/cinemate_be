package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.CreateSubscriptionRequest;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionResponse;
import com.pbl6.cinemate.payment_service.service.SubscriptionService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    public ResponseEntity<ResponseData> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        SubscriptionResponse subscription = subscriptionService.createSubscription(request);
        return ResponseEntity.ok(ResponseData.success(
                subscription,
                "Subscription created successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/current/{userId}")
    public ResponseEntity<ResponseData> getCurrentSubscription(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        SubscriptionResponse subscription = subscriptionService.getCurrentSubscription(userId);
        return ResponseEntity.ok(ResponseData.success(
                subscription,
                "Current subscription retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/history/{userId}")
    public ResponseEntity<ResponseData> getSubscriptionHistory(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        List<SubscriptionResponse> history = subscriptionService.getSubscriptionHistory(userId);
        return ResponseEntity.ok(ResponseData.success(
                history,
                "Subscription history retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ResponseData> cancelSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam UUID userId,
            HttpServletRequest httpRequest) {
        SubscriptionResponse subscription = subscriptionService.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.ok(ResponseData.success(
                subscription,
                "Subscription cancelled successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @PostMapping("/renew")
    public ResponseEntity<ResponseData> renewSubscription(
            @RequestParam UUID userId,
            @RequestParam UUID planId,
            HttpServletRequest httpRequest) {
        SubscriptionResponse subscription = subscriptionService.renewSubscription(userId, planId);
        return ResponseEntity.ok(ResponseData.success(
                subscription,
                "Subscription renewed successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
}
