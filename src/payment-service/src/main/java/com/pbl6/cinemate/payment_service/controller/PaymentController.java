package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.CreatePaymentRequest;
import com.pbl6.cinemate.payment_service.dto.response.PaymentResponse;
import com.pbl6.cinemate.payment_service.dto.response.PaymentUrlResponse;
import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.service.PaymentService;
import com.pbl6.cinemate.payment_service.service.SubscriptionService;
import com.pbl6.cinemate.payment_service.service.VNPayService;
import com.pbl6.cinemate.payment_service.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    private final VNPayService vnPayService;
    private final SubscriptionService subscriptionService;
    
    @PostMapping("/create-url")
    public ResponseEntity<PaymentUrlResponse> createPaymentUrl(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        
        // Create payment record
        Payment payment = paymentService.createPayment(request);
        
        // Get IP address
        String ipAddress = VNPayUtil.getIpAddress(
                httpRequest.getHeader("X-Forwarded-For"),
                httpRequest.getRemoteAddr()
        );
        
        // Generate VNPay payment URL
        PaymentUrlResponse response = vnPayService.createPaymentUrl(payment, ipAddress);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, Object>> vnPayReturn(@RequestParam Map<String, String> params) {
        try {
            // Process payment callback
            Payment payment = vnPayService.processPaymentCallback(params);
            
            // If payment successful, activate subscription
            if (payment.getStatus().toString().equals("SUCCESS")) {
                subscriptionService.activateSubscription(payment.getSubscription().getId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", payment.getStatus());
            response.put("message", vnPayService.getPaymentStatus(payment.getVnpResponseCode()));
            response.put("transactionId", payment.getVnpTransactionNo());
            response.put("amount", payment.getAmount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing VNPay return", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Payment processing failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnPayIPN(@RequestParam Map<String, String> params) {
        try {
            // Process payment callback
            Payment payment = vnPayService.processPaymentCallback(params);
            
            // If payment successful, activate subscription
            if (payment.getStatus().toString().equals("SUCCESS")) {
                subscriptionService.activateSubscription(payment.getSubscription().getId());
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("RspCode", "99");
            errorResponse.put("Message", "Unknown error");
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@PathVariable Long userId) {
        List<PaymentResponse> history = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        PaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
}
