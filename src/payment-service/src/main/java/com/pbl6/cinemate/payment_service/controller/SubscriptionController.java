package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.CreateSubscriptionRequest;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionResponse;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionWithPaymentResponse;
import com.pbl6.cinemate.payment_service.service.SubscriptionService;
import com.pbl6.cinemate.payment_service.util.VNPayUtil;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

        private final SubscriptionService subscriptionService;

        @PostMapping
        public ResponseEntity<ResponseData> createSubscription(
                        @Valid @RequestBody CreateSubscriptionRequest request,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                // Extract IP address from request
                String ipAddress = VNPayUtil.getIpAddress(
                                httpRequest.getHeader("X-Forwarded-For"),
                                httpRequest.getRemoteAddr());

                // Get userId and email from authenticated user
                UUID userId = userPrincipal.getId();
                String userEmail = userPrincipal.getUsername(); // Returns email

                // Create subscription with auto-generated payment URL
                SubscriptionWithPaymentResponse response = subscriptionService.createSubscriptionWithPayment(
                                request, userId, userEmail, ipAddress);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Subscription and payment URL created successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/current")
        public ResponseEntity<ResponseData> getCurrentSubscription(
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                SubscriptionResponse subscription = subscriptionService.getCurrentSubscription(userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                subscription,
                                "Current subscription retrieved successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/history")
        public ResponseEntity<ResponseData> getSubscriptionHistory(
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                List<SubscriptionResponse> history = subscriptionService.getSubscriptionHistory(userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                history,
                                "Subscription history retrieved successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @PutMapping("/{subscriptionId}/cancel")
        public ResponseEntity<ResponseData> cancelSubscription(
                        @PathVariable UUID subscriptionId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                SubscriptionResponse subscription = subscriptionService.cancelSubscription(subscriptionId,
                                userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                subscription,
                                "Subscription cancelled successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @PostMapping("/renew")
        public ResponseEntity<ResponseData> renewSubscription(
                        @CurrentUser UserPrincipal userPrincipal,
                        @RequestParam(name = "planId") UUID planId,
                        HttpServletRequest httpRequest) {
                SubscriptionResponse subscription = subscriptionService.renewSubscription(userPrincipal.getId(),
                                planId);
                return ResponseEntity.ok(ResponseData.success(
                                subscription,
                                "Subscription renewed successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }
}
