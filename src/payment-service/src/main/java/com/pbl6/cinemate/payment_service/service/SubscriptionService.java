package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.request.CreatePaymentRequest;
import com.pbl6.cinemate.payment_service.dto.request.CreateSubscriptionRequest;
import com.pbl6.cinemate.payment_service.dto.response.PaymentUrlResponse;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionPlanResponse;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionResponse;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionWithPaymentResponse;
import com.pbl6.cinemate.payment_service.email.PaymentEmailService;
import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.entity.SubscriptionPlan;
import com.pbl6.cinemate.payment_service.enums.PaymentMethod;
import com.pbl6.cinemate.payment_service.enums.SubscriptionStatus;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.exception.SubscriptionException;
import com.pbl6.cinemate.payment_service.repository.FamilyMemberRepository;
import com.pbl6.cinemate.payment_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanService planService;
    private final PaymentService paymentService;
    private final VNPayService vnPayService;
    private final ModelMapper modelMapper;
    private final FamilyMemberRepository familyMemberRepository;
    private final PaymentEmailService paymentEmailService;
    
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, UUID userId) {
        // Check if user already has an active subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findActiveSubscriptionByUserId(userId);
        if (existingSubscription.isPresent()) {
            throw new SubscriptionException("User already has an active subscription");
        }
        
        // Get subscription plan
        SubscriptionPlan plan = planService.getPlanEntityById(request.getPlanId());
        
        // Create subscription with server-controlled userId
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(request.getAutoRenew());
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Created subscription with ID: {} for user: {}", savedSubscription.getId(), userId);
        
        return mapToResponse(savedSubscription);
    }
    
    /**
     * Create subscription and automatically generate payment URL
     * This is the recommended method as it ensures amount and email are server-side controlled
     */
    @Transactional
    public SubscriptionWithPaymentResponse createSubscriptionWithPayment(
            CreateSubscriptionRequest request,
            UUID userId,
            String userEmail,
            String ipAddress) {
        
        // Check if user already has an active subscription
        Optional<Subscription> existingSubscription = 
            subscriptionRepository.findActiveSubscriptionByUserId(userId);
        if (existingSubscription.isPresent()) {
            throw new SubscriptionException("User already has an active subscription");
        }
        
        // Get subscription plan
        SubscriptionPlan plan = planService.getPlanEntityById(request.getPlanId());
        
        // Create subscription with server-controlled userId
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(request.getAutoRenew());
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        log.info("Created subscription with ID: {} for user: {}", 
            savedSubscription.getId(), userId);
        
        // Create payment request internally (server-side controlled)
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setSubscriptionId(savedSubscription.getId());
        paymentRequest.setAmount(plan.getPrice());  // Amount from server-side plan
        paymentRequest.setPaymentMethod(PaymentMethod.VNPAY);
        paymentRequest.setOrderInfo(plan.getName() + " plan for " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        
        // Create payment entity with server-controlled userId and userEmail
        Payment payment = paymentService.createPayment(paymentRequest, userId, userEmail);
        log.info("Created payment with ID: {} for subscription: {}", 
            payment.getId(), savedSubscription.getId());
        
        // Generate VNPay payment URL
        PaymentUrlResponse paymentUrl = vnPayService.createPaymentUrl(payment, ipAddress);
        
        // Map plan to response
        SubscriptionPlanResponse planResponse = modelMapper.map(plan, SubscriptionPlanResponse.class);
        
        // Build combined response
        return SubscriptionWithPaymentResponse.builder()
            .subscriptionId(savedSubscription.getId())
            .userId(savedSubscription.getUserId())
            .plan(planResponse)
            .status(savedSubscription.getStatus())
            .autoRenew(savedSubscription.getAutoRenew())
            .createdAt(savedSubscription.getCreatedAt())
            .paymentId(payment.getId())
            .paymentUrl(paymentUrl.getPaymentUrl())
            .vnpTxnRef(paymentUrl.getVnpTxnRef())
            .amount(plan.getPrice())
            .message("Subscription created successfully. Please complete payment to activate.")
            .build();
    }
    
    @Transactional
    public SubscriptionResponse activateSubscription(UUID subscriptionId, Payment payment) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));
        
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new SubscriptionException("Only pending subscriptions can be activated");
        }
        
        // Set subscription dates
        LocalDateTime now = LocalDateTime.now();
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusDays(subscription.getPlan().getDurationDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        
        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Activated subscription: {}", subscriptionId);

        if(updatedSubscription.getPlan().getIsFamilyPlan())
        {
            FamilyMember familyMember = new FamilyMember();
            familyMember.setIsOwner(true);
            familyMember.setSubscription(updatedSubscription);
            familyMember.setUserId(updatedSubscription.getUserId());
            familyMember.setJoinedAt(now);
            familyMemberRepository.save(familyMember);
        }
        
        // Send activation emails
        sendActivationEmails(updatedSubscription, payment);
        
        return mapToResponse(updatedSubscription);
    }
    
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, UUID userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));
        
        if (!subscription.getUserId().equals(userId)) {
            throw new SubscriptionException("You can only cancel your own subscription");
        }
        
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionException("Only active subscriptions can be cancelled");
        }
        
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        
        Subscription updatedSubscription = subscriptionRepository.save(subscription);
        log.info("Cancelled subscription: {}", subscriptionId);
        
        return mapToResponse(updatedSubscription);
    }
    
    @Transactional
    public SubscriptionResponse renewSubscription(UUID userId, UUID planId) {
        // Cancel any existing active subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findActiveSubscriptionByUserId(userId);
        existingSubscription.ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
        });
        
        // Create new subscription
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPlanId(planId);
        request.setAutoRenew(true);
        
        return createSubscription(request, userId);
    }
    
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(UUID userId) {
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription not found for user: " + userId));
        return mapToResponse(subscription);
    }
    
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionHistory(UUID userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(UUID userId) {
        return subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }
    
    @Transactional
    public void expireOldSubscriptions() {
        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredSubscriptions(
                SubscriptionStatus.ACTIVE, 
                LocalDateTime.now()
        );
        
        expiredSubscriptions.forEach(subscription -> {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            log.info("Expired subscription: {}", subscription.getId());
        });
    }
    
    private SubscriptionResponse mapToResponse(Subscription subscription) {
        SubscriptionResponse response = modelMapper.map(subscription, SubscriptionResponse.class);
        response.setPlan(modelMapper.map(subscription.getPlan(), com.pbl6.cinemate.payment_service.dto.response.SubscriptionPlanResponse.class));
        return response;
    }
    
    /**
     * Send payment success and subscription activated emails
     * Email failure will not break the transaction
     */
    private void sendActivationEmails(Subscription subscription, Payment payment) {
        try {
            // Use email from payment (captured at payment creation time)
            String recipientEmail = payment.getUserEmail();
            String recipientName = payment.getUserEmail(); // Use email as name
            
            log.info("Sending activation emails to: {}", recipientEmail);
            
            // Send payment success email
            paymentEmailService.sendPaymentSuccessEmail(
                    recipientEmail,
                    recipientName,
                    payment,
                    subscription.getPlan()
            );
            
            // Send subscription activated email
            paymentEmailService.sendSubscriptionActivatedEmail(
                    recipientEmail,
                    recipientName,
                    subscription
            );
            
            log.info("Activation emails sent successfully for subscription: {}", subscription.getId());
            
        } catch (Exception e) {
            log.error("Failed to send activation emails for subscription {}: {}", 
                    subscription.getId(), e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break subscription activation
        }
    }
}
