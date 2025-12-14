package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.response.ContentAccessResponse;
import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import com.pbl6.cinemate.payment_service.entity.ParentControl;
import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.enums.SubscriptionStatus;
import com.pbl6.cinemate.payment_service.repository.FamilyMemberRepository;
import com.pbl6.cinemate.payment_service.repository.ParentControlRepository;
import com.pbl6.cinemate.payment_service.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentAccessService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final ParentControlRepository parentControlRepository;
    
    public ContentAccessResponse checkContentAccess(
            UUID userId, 
            List<UUID> movieCategoryIds, 
            Integer currentWatchTimeMinutes) {
        
        log.info("Checking content access for user: {}, categoryIds: {}, watchTime: {}", 
                userId, movieCategoryIds, currentWatchTimeMinutes);
        
        Subscription subscription;
        
        // Step 1: Check if user is a FAMILY MEMBER first
        List<FamilyMember> familyMembers = familyMemberRepository.findByUserId(userId);
        
        if (!familyMembers.isEmpty()) {
            // User is a family member - get subscription from family member
            FamilyMember member = familyMembers.get(0);
            subscription = member.getSubscription();
            log.debug("User {} is a family member of subscription {}", userId, subscription.getId());
        } else {
            // User is NOT a family member - check for regular subscription
            Optional<Subscription> regularSub = subscriptionRepository
                    .findActiveSubscriptionByUserId(userId);
            
            if (regularSub.isEmpty()) {
                log.warn("No active subscription found for user: {}", userId);
                return denied("No active subscription found");
            }
            
            subscription = regularSub.get();
            log.debug("User {} has a regular subscription {}", userId, subscription.getId());
        }
        
        // Step 2: Verify subscription is ACTIVE
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            log.warn("Subscription {} is not active (status: {})", subscription.getId(), subscription.getStatus());
            return denied("Subscription is not active");
        }
        
        // Check if subscription has expired (additional safety check)
        if (subscription.getEndDate() != null && 
            subscription.getEndDate().isBefore(java.time.LocalDateTime.now())) {
            log.warn("Subscription {} has expired (end date: {})", subscription.getId(), subscription.getEndDate());
            return denied("Subscription has expired");
        }
        
        log.debug("Subscription {} is active and valid", subscription.getId());
        
        // Step 3: Check if user is a kid (has parent control)
        List<ParentControl> parentControls = parentControlRepository.findByKidId(userId);
        
        if (parentControls.isEmpty()) {
            // Not a kid - full access (Premium subscriber, Family Owner, or Adult Member)
            log.debug("User {} has full access - no parental controls", userId);
            return allowed(false);
        }
        
        // Step 4: Apply parental controls (this is a kid)
        log.info("User {} is a kid with parental controls - applying restrictions", userId);
        ParentControl control = parentControls.get(0);
        
        return applyParentalControls(control, movieCategoryIds, currentWatchTimeMinutes, userId);
    }
    
    /**
     * Apply parental control restrictions for a kid user
     */
    private ContentAccessResponse applyParentalControls(
            ParentControl control,
            List<UUID> movieCategoryIds,
            Integer currentWatchTimeMinutes,
            UUID userId) {
        
        List<UUID> blockedCategoryIds = parseBlockedCategoryIds(control.getBlockedCategories());
        
        // Check if any movie category is blocked
        for (UUID categoryId : movieCategoryIds) {
            if (isCategoryIdBlocked(categoryId, blockedCategoryIds)) {
                log.warn("Content blocked for kid {}: category '{}' is restricted", userId, categoryId);
                return ContentAccessResponse.builder()
                        .allowed(false)
                        .reason(String.format("Content restricted by parent: category %s is blocked", categoryId))
                        .isKid(true)
                        .blockedCategoryIds(blockedCategoryIds)
                        .remainingWatchTimeMinutes(calculateRemainingTime(control, currentWatchTimeMinutes))
                        .build();
            }
        }
        
        // Check watch time limit
        Integer watchTimeLimit = control.getWatchTimeLimitMinutes();
        
        if (watchTimeLimit != null && currentWatchTimeMinutes >= watchTimeLimit) {
            log.warn("Watch time limit exceeded for kid {}: {}/{} minutes", 
                    userId, currentWatchTimeMinutes, watchTimeLimit);
            return ContentAccessResponse.builder()
                    .allowed(false)
                    .reason(String.format("Daily watch time limit reached (%d/%d minutes)", 
                            currentWatchTimeMinutes, watchTimeLimit))
                    .isKid(true)
                    .blockedCategoryIds(blockedCategoryIds)
                    .remainingWatchTimeMinutes(0)
                    .build();
        }
        
        // All checks passed - kid can watch
        log.info("Content access granted for kid {}", userId);
        return ContentAccessResponse.builder()
                .allowed(true)
                .isKid(true)
                .blockedCategoryIds(blockedCategoryIds)
                .remainingWatchTimeMinutes(calculateRemainingTime(control, currentWatchTimeMinutes))
                .build();
    }
    
    /**
     * Helper method to build a denied response
     */
    private ContentAccessResponse denied(String reason) {
        return ContentAccessResponse.builder()
                .allowed(false)
                .reason(reason)
                .isKid(false)
                .build();
    }
    
    /**
     * Helper method to build an allowed response
     */
    private ContentAccessResponse allowed(boolean isKid) {
        return ContentAccessResponse.builder()
                .allowed(true)
                .isKid(isKid)
                .build();
    }
    
    /**
     * Parse comma-separated blocked category UUIDs string into a list
     */
    private List<UUID> parseBlockedCategoryIds(String blockedCategoriesStr) {
        if (blockedCategoriesStr == null || blockedCategoriesStr.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(blockedCategoriesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a movie category UUID is in the blocked list
     */
    private boolean isCategoryIdBlocked(UUID categoryId, List<UUID> blockedCategoryIds) {
        return blockedCategoryIds.contains(categoryId);
    }
    
    /**
     * Calculate remaining watch time for the kid
     */
    private Integer calculateRemainingTime(ParentControl control, Integer currentWatchTimeMinutes) {
        Integer limit = control.getWatchTimeLimitMinutes();
        if (limit == null) {
            return null; // No limit set
        }
        int remaining = limit - currentWatchTimeMinutes;
        return Math.max(0, remaining);
    }
}
