package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.response.SubscriptionPlanResponse;
import com.pbl6.cinemate.payment_service.service.SubscriptionPlanService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {
    
    private final SubscriptionPlanService planService;
    
    @GetMapping
    public ResponseEntity<ResponseData> getAllPlans(HttpServletRequest httpRequest) {
        List<SubscriptionPlanResponse> plans = planService.getAllPlans();
        return ResponseEntity.ok(ResponseData.success(
                plans,
                "Subscription plans retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getPlanById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        SubscriptionPlanResponse plan = planService.getPlanById(id);
        return ResponseEntity.ok(ResponseData.success(
                plan,
                "Subscription plan retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
}
