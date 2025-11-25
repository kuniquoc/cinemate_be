package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.CreateSubscriptionRequest;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionResponse;
import com.pbl6.cinemate.payment_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse subscription = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }
    
    @GetMapping("/current/{userId}")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(@PathVariable Long userId) {
        SubscriptionResponse subscription = subscriptionService.getCurrentSubscription(userId);
        return ResponseEntity.ok(subscription);
    }
    
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionHistory(@PathVariable Long userId) {
        List<SubscriptionResponse> history = subscriptionService.getSubscriptionHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @PutMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam Long userId) {
        SubscriptionResponse subscription = subscriptionService.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.ok(subscription);
    }
    
    @PostMapping("/renew")
    public ResponseEntity<SubscriptionResponse> renewSubscription(
            @RequestParam Long userId,
            @RequestParam Long planId) {
        SubscriptionResponse subscription = subscriptionService.renewSubscription(userId, planId);
        return ResponseEntity.ok(subscription);
    }
}
