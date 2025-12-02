package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.request.CreateSubscriptionRequest;
import com.pbl6.cinemate.payment_service.dto.response.SubscriptionResponse;
import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.entity.SubscriptionPlan;
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
    private final ModelMapper modelMapper;
    private final FamilyMemberRepository familyMemberRepository;
    
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        // Check if user already has an active subscription
        Optional<Subscription> existingSubscription = subscriptionRepository.findActiveSubscriptionByUserId(request.getUserId());
        if (existingSubscription.isPresent()) {
            throw new SubscriptionException("User already has an active subscription");
        }
        
        // Get subscription plan
        SubscriptionPlan plan = planService.getPlanEntityById(request.getPlanId());
        
        // Create subscription
        Subscription subscription = new Subscription();
        subscription.setUserId(request.getUserId());
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(request.getAutoRenew());
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Created subscription with ID: {} for user: {}", savedSubscription.getId(), request.getUserId());
        
        return mapToResponse(savedSubscription);
    }
    
    @Transactional
    public SubscriptionResponse activateSubscription(UUID subscriptionId) {
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
        request.setUserId(userId);
        request.setPlanId(planId);
        request.setAutoRenew(true);
        
        return createSubscription(request);
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
}
