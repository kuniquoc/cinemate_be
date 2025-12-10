package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.CreatePaymentRequest;
import com.pbl6.cinemate.payment_service.dto.response.PaymentResponse;
import com.pbl6.cinemate.payment_service.dto.response.PaymentUrlResponse;
import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.service.PaymentService;
import com.pbl6.cinemate.payment_service.service.SubscriptionService;
import com.pbl6.cinemate.payment_service.service.VNPayService;
import com.pbl6.cinemate.payment_service.util.VNPayUtil;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    private final VNPayService vnPayService;
    private final SubscriptionService subscriptionService;
    
    @PostMapping("/create-url")
    public ResponseEntity<ResponseData> createPaymentUrl(
            @Valid @RequestBody CreatePaymentRequest request,
            @CurrentUser UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {
        
        // Extract userId and userEmail from authenticated user
        UUID userId = userPrincipal.getId();
        String userEmail = userPrincipal.getUsername();
        
        // Create payment record with server-controlled userId and userEmail
        Payment payment = paymentService.createPayment(request, userId, userEmail);
        
        // Get IP address
        String ipAddress = VNPayUtil.getIpAddress(
                httpRequest.getHeader("X-Forwarded-For"),
                httpRequest.getRemoteAddr()
        );
        
        // Generate VNPay payment URL
        PaymentUrlResponse response = vnPayService.createPaymentUrl(payment, ipAddress);
        
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Payment URL created successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/vnpay-return")
    public ResponseEntity<ResponseData> vnPayReturn(
            @RequestParam Map<String, String> params,
            HttpServletRequest httpRequest) {
        try {
            // Process payment callback
            Payment payment = vnPayService.processPaymentCallback(params);
            
            // If payment successful, activate subscription and send emails
            if (payment.getStatus().toString().equals("SUCCESS")) {
                subscriptionService.activateSubscription(payment.getSubscription().getId(), payment);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", payment.getStatus());
            response.put("message", vnPayService.getPaymentStatus(payment.getVnpResponseCode()));
            response.put("transactionId", payment.getVnpTransactionNo());
            response.put("amount", payment.getAmount());
            
            return ResponseEntity.ok(ResponseData.success(
                    response,
                    "Payment processed successfully",
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod()));
            
        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            throw e;
        }
    }
    
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<ResponseData> vnPayIPN(
            @RequestParam Map<String, String> params,
            HttpServletRequest httpRequest) {
        try {
            // Process payment callback
            Payment payment = vnPayService.processPaymentCallback(params);
            
            // If payment successful, activate subscription and send emails
            if (payment.getStatus().toString().equals("SUCCESS")) {
                subscriptionService.activateSubscription(payment.getSubscription().getId(), payment);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            
            return ResponseEntity.ok(ResponseData.success(
                    response,
                    "IPN processed successfully",
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod()));
            
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("RspCode", "99");
            errorResponse.put("Message", "Unknown error");
            return ResponseEntity.ok(ResponseData.success(
                    errorResponse,
                    "IPN processing failed",
                    httpRequest.getRequestURI(),
                    httpRequest.getMethod()));
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<ResponseData> getPaymentHistory(
            @CurrentUser UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {
        List<PaymentResponse> history = paymentService.getPaymentHistory(userPrincipal.getId());
        return ResponseEntity.ok(ResponseData.success(
                history,
                "Payment history retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getPaymentById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        PaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ResponseData.success(
                payment,
                "Payment retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
}
