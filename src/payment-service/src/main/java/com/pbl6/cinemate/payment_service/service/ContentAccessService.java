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

import java.time.Instant;
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

        // Step 1: Determine subscription source (family member or personal subscriber)
        SubscriptionSource subscriptionSource = determineSubscriptionSource(userId);
        
        if (subscriptionSource == null) {
            log.warn("No active subscription or family membership found for user: {}", userId);
            return denied("No active subscription found. Please subscribe to access content.");
        }

        // Step 2: Validate subscription status
        ContentAccessResponse validationResult = validateSubscription(
                subscriptionSource.subscription, 
                subscriptionSource.isFamilyMember);
        
        if (!validationResult.getAllowed()) {
            return validationResult;
        }

        log.debug("Subscription {} is active and valid for user {}", 
                subscriptionSource.subscription.getId(), userId);

        // Step 3: Check if user is a kid (has parental controls)
        List<ParentControl> parentControls = parentControlRepository.findByKidId(userId);

        if (parentControls.isEmpty()) {
            // Not a kid - grant full access
            log.info("User {} granted full access - no parental controls", userId);
            return allowed(false);
        }

        // Step 4: Apply parental controls (user is a kid)
        log.info("User {} is a kid with parental controls - applying restrictions", userId);
        ParentControl control = parentControls.get(0);

        return applyParentalControls(control, movieCategoryIds, currentWatchTimeMinutes, userId);
    }

    /**
     * Determine if user accesses content via family membership or personal subscription
     */
    private SubscriptionSource determineSubscriptionSource(UUID userId) {
        // Check if user is a family member first
        List<FamilyMember> familyMembers = familyMemberRepository.findByUserId(userId);

        if (!familyMembers.isEmpty()) {
            // User is a family member - get subscription from family
            FamilyMember member = familyMembers.get(0);
            Subscription subscription = member.getSubscription();
            log.debug("User {} is a family member (owner: {}) of subscription {}", 
                    userId, member.getIsOwner(), subscription.getId());
            return new SubscriptionSource(subscription, true, member.getIsOwner());
        }

        // User is NOT a family member - check for personal subscription
        Optional<Subscription> personalSub = subscriptionRepository
                .findActiveSubscriptionByUserId(userId);

        if (personalSub.isEmpty()) {
            log.warn("No active personal subscription found for user: {}", userId);
            return null;
        }

        log.debug("User {} has a personal subscription {}", userId, personalSub.get().getId());
        return new SubscriptionSource(personalSub.get(), false, false);
    }

    /**
     * Validate that subscription is active and not expired
     */
    private ContentAccessResponse validateSubscription(Subscription subscription, boolean isFamilyMember) {
        String subscriptionType = isFamilyMember ? "Family plan" : "Personal";

        // Check if subscription status is ACTIVE
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            log.warn("{} subscription {} is not active (status: {})", 
                    subscriptionType, subscription.getId(), subscription.getStatus());
            
            String message = isFamilyMember 
                    ? "Family plan subscription is inactive. Please contact family owner."
                    : String.format("Personal subscription is %s. Please reactivate or subscribe.", 
                            subscription.getStatus().toString().toLowerCase());
            
            return denied(message);
        }

        // Check if subscription has expired
        if (subscription.getEndDate() != null &&
                subscription.getEndDate().isBefore(Instant.now())) {
            log.warn("{} subscription {} has expired (end date: {})", 
                    subscriptionType, subscription.getId(), subscription.getEndDate());
            
            String message = isFamilyMember
                    ? "Family plan subscription has expired. Please contact family owner."
                    : String.format("Personal subscription expired on %s. Please renew.", 
                            subscription.getEndDate().toString());
            
            return denied(message);
        }

        // Subscription is valid
        return allowed(false);
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
                        .reason(String.format("Content category '%s' is blocked by parental controls.", categoryId))
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
                    .reason(String.format("Daily watch time limit reached (%d/%d minutes). Try again tomorrow.",
                            currentWatchTimeMinutes, watchTimeLimit))
                    .isKid(true)
                    .blockedCategoryIds(blockedCategoryIds)
                    .remainingWatchTimeMinutes(0)
                    .build();
        }

        // All checks passed - kid can watch
        log.info("Content access granted for kid {} with parental controls", userId);
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

    /**
     * Internal class to hold subscription source information
     */
    private static class SubscriptionSource {
        final Subscription subscription;
        final boolean isFamilyMember;
        final boolean isOwner;

        SubscriptionSource(Subscription subscription, boolean isFamilyMember, boolean isOwner) {
            this.subscription = subscription;
            this.isFamilyMember = isFamilyMember;
            this.isOwner = isOwner;
        }
    }
}
