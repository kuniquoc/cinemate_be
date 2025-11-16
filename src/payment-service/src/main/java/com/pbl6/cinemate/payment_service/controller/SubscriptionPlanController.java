package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.response.SubscriptionPlanResponse;
import com.pbl6.cinemate.payment_service.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {
    
    private final SubscriptionPlanService planService;
    
    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans() {
        List<SubscriptionPlanResponse> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(@PathVariable Long id) {
        SubscriptionPlanResponse plan = planService.getPlanById(id);
        return ResponseEntity.ok(plan);
    }
}
