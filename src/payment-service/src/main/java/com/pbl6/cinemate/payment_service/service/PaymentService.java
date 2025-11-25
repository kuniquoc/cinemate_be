package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.request.CreatePaymentRequest;
import com.pbl6.cinemate.payment_service.dto.response.PaymentResponse;
import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.repository.PaymentRepository;
import com.pbl6.cinemate.payment_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ModelMapper modelMapper;
    
    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        // Verify subscription exists
        Subscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", request.getSubscriptionId()));
        
        // Create payment entity
        Payment payment = new Payment();
        payment.setUserId(request.getUserId());
        payment.setSubscription(subscription);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        
        // Generate unique transaction reference
        String vnpTxnRef = generateTransactionRef();
        payment.setVnpTxnRef(vnpTxnRef);
        payment.setVnpOrderInfo(request.getOrderInfo() != null ? 
                request.getOrderInfo() : 
                "Payment for subscription " + subscription.getId());
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created payment with ID: {} for user: {}", savedPayment.getId(), request.getUserId());
        
        return savedPayment;
    }
    
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        
        payment.setStatus(status);
        if (status == PaymentStatus.SUCCESS) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        
        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Updated payment {} status to: {}", paymentId, status);
        
        return updatedPayment;
    }
    
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(payment -> modelMapper.map(payment, PaymentResponse.class))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return modelMapper.map(payment, PaymentResponse.class);
    }
    
    @Transactional(readOnly = true)
    public Payment getPaymentByVnpTxnRef(String vnpTxnRef) {
        return paymentRepository.findByVnpTxnRef(vnpTxnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "vnpTxnRef", vnpTxnRef));
    }
    
    private String generateTransactionRef() {
        // Generate format: VNPAY_YYYYMMDDHHMMSS_RANDOM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "VNPAY_" + timestamp + "_" + random;
    }
}
