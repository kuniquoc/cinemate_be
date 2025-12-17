package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.client.AuthServiceClient;
import com.pbl6.cinemate.payment_service.dto.response.FamilyMemberDetailResponse;
import com.pbl6.cinemate.payment_service.email.FamilyInvitationEmailService;
import com.pbl6.cinemate.payment_service.entity.*;
import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import com.pbl6.cinemate.payment_service.exception.DeviceLimitException;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.exception.SubscriptionException;
import com.pbl6.cinemate.payment_service.repository.*;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyPlanService {

    private final SubscriptionRepository subscriptionRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository invitationRepository;
    private final ParentControlRepository parentControlRepository;
    private final FamilyInvitationEmailService emailService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public FamilyInvitation createInvitation(
            UUID userId,
            InvitationMode mode,
            String inviterEmail,
            String recipientEmail,
            Boolean sendEmail) {

        // Find user's active subscription
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(
                        "You don't have an active subscription. Please subscribe to a family plan first"));

        // Verify subscription is a family plan
        if (!subscription.getPlan().getIsFamilyPlan()) {
            throw new SubscriptionException(
                    "Your current subscription is not a family plan. Please upgrade to family plan to invite members");
        }

        // Verify user is registered as a family member
        FamilyMember familyMember = familyMemberRepository.findBySubscriptionIdAndUserId(subscription.getId(), userId)
                .orElseThrow(() -> new SubscriptionException(
                        "You are not registered as a member of this family plan. Please contact support"));

        // Verify user is the owner
        if (!familyMember.getIsOwner()) {
            throw new SubscriptionException(
                    "Only the family plan owner can send invitations. Please ask the owner to invite new members");
        }

        // Check if subscription has available slots
        Long currentMembers = familyMemberRepository.countBySubscriptionId(subscription.getId());
        Integer maxMembers = subscription.getPlan().getMaxMembers();

        if (currentMembers >= maxMembers) {
            throw new DeviceLimitException("Family plan has reached maximum member limit");
        }

        // Generate unique invitation token
        String token = UUID.randomUUID().toString();

        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setSubscription(subscription);
        invitation.setInvitationToken(token);
        invitation.setMode(mode);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(userId);
        invitation.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60L)); // 7 days validity

        FamilyInvitation savedInvitation = invitationRepository.save(invitation);

        // Send email if requested
        if (sendEmail != null && sendEmail && recipientEmail != null && !recipientEmail.isEmpty()) {
            String invitationLink = frontendUrl + "/family/join?token=" + token;
            emailService.sendInvitationEmail(
                    inviterEmail,
                    recipientEmail,
                    invitationLink,
                    mode,
                    savedInvitation.getExpiresAt());
        }

        return savedInvitation;
    }

    @Transactional
    public FamilyMember acceptInvitation(String token, UUID userId, String userEmail) {
        // Find invitation
        FamilyInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Validate invitation
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new SubscriptionException("Invitation is no longer valid");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new SubscriptionException("Invitation has expired");
        }

        // Validate that the accepting user's email matches the invited email
        if (invitation.getRecipientEmail() != null &&
                !invitation.getRecipientEmail().equalsIgnoreCase(userEmail)) {
            throw new SubscriptionException(
                    "This invitation was sent to a different email address. Please use the account associated with the invited email: "
                            + invitation.getRecipientEmail());
        }

        Subscription subscription = invitation.getSubscription();

        // Check if user is already a member
        if (familyMemberRepository.existsBySubscriptionIdAndUserId(subscription.getId(), userId)) {
            throw new SubscriptionException("User is already a member of this family plan");
        }

        // Check member limit
        Long currentMembers = familyMemberRepository.countBySubscriptionId(subscription.getId());
        if (currentMembers >= subscription.getPlan().getMaxMembers()) {
            throw new DeviceLimitException("Family plan has reached maximum member limit");
        }

        // Create family member
        FamilyMember member = new FamilyMember();
        member.setSubscription(subscription);
        member.setUserId(userId);
        member.setIsOwner(false);
        member.setJoinedAt(Instant.now());

        FamilyMember savedMember = familyMemberRepository.save(member);

        // If KID mode, create parent control record
        if (invitation.getMode() == InvitationMode.KID) {
            ParentControl parentControl = new ParentControl();
            parentControl.setParentId(invitation.getInvitedBy());
            parentControl.setKidId(userId);
            parentControl.setSubscription(subscription);
            parentControlRepository.save(parentControl);
        }

        // Update invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setInvitedUserId(userId);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        return savedMember;
    }

    public List<FamilyMember> getFamilyMembers(UUID subscriptionId) {
        return familyMemberRepository.findBySubscriptionId(subscriptionId);
    }

    public List<FamilyInvitation> getInvitations(UUID subscriptionId) {
        return invitationRepository.findBySubscriptionId(subscriptionId);
    }

    @Transactional
    public void removeMember(UUID ownerId, UUID memberUserId) {
        // Find owner's active subscription
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(ownerId)
                .orElseThrow(() -> new SubscriptionException("You don't have an active subscription"));

        // Verify user is the owner of the subscription
        FamilyMember owner = familyMemberRepository.findBySubscriptionIdAndUserId(subscription.getId(), ownerId)
                .orElseThrow(() -> new SubscriptionException("User is not a member of this subscription"));

        if (!owner.getIsOwner()) {
            throw new SubscriptionException("Only the plan owner can remove members");
        }

        // Prevent removing self
        if (ownerId.equals(memberUserId)) {
            throw new SubscriptionException("Cannot remove yourself from the family plan");
        }

        // Find and remove member
        FamilyMember member = familyMemberRepository.findBySubscriptionIdAndUserId(subscription.getId(), memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in your family plan"));

        if (member.getIsOwner()) {
            throw new SubscriptionException("Cannot remove the plan owner");
        }

        // Remove parent control records if exists
        parentControlRepository.findByKidIdAndSubscriptionId(memberUserId, subscription.getId())
                .ifPresent(parentControlRepository::delete);

        familyMemberRepository.delete(member);
    }

    @Transactional
    public void cancelInvitation(UUID invitationId, UUID ownerId) {
        FamilyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getInvitedBy().equals(ownerId)) {
            throw new SubscriptionException("Only the invitation creator can cancel it");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
    }

    public ParentControl getParentControl(UUID parentId, UUID kidId) {
        return parentControlRepository.findByParentIdAndKidId(parentId, kidId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent control not found"));
    }

    @Transactional
    public ParentControl updateParentControl(UUID parentId, UUID kidId, List<UUID> blockedCategoryIds,
            Integer watchTimeLimit) {
        ParentControl control = parentControlRepository.findByParentIdAndKidId(parentId, kidId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent control not found"));

        if (blockedCategoryIds != null) {
            // Convert UUIDs to comma-separated string
            String categoryIdsString = blockedCategoryIds.stream()
                    .map(UUID::toString)
                    .collect(java.util.stream.Collectors.joining(","));
            log.info("Setting blocked categories for parent {} kid {}: {}", parentId, kidId, categoryIdsString);
            control.setBlockedCategories(categoryIdsString);
        }

        if (watchTimeLimit != null) {
            control.setWatchTimeLimitMinutes(watchTimeLimit);
        }

        return parentControlRepository.save(control);
    }

    public List<ParentControl> getKidsForParent(UUID parentId) {
        return parentControlRepository.findByParentId(parentId);
    }

    public List<FamilyMemberDetailResponse> getCurrentUserFamilyMembers(UUID userId) {
        // Find user's active subscription
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(userId)
                .orElseThrow(() -> new SubscriptionException("You don't have an active subscription"));

        // Verify subscription is a family plan
        if (!subscription.getPlan().getIsFamilyPlan()) {
            throw new SubscriptionException("Your current subscription is not a family plan");
        }

        // Verify user is a member of this family plan
        familyMemberRepository.findBySubscriptionIdAndUserId(subscription.getId(), userId)
                .orElseThrow(() -> new SubscriptionException("You are not a member of this family plan"));

        // Get all family members
        List<FamilyMember> members = familyMemberRepository.findBySubscriptionId(subscription.getId());

        // Build detailed responses
        List<FamilyMemberDetailResponse> responses = new ArrayList<>();
        for (FamilyMember member : members) {
            String email = "N/A";
            try {
                ResponseData response = authServiceClient.getEmailByUserId(member.getUserId());
                if (response != null && response.getData() != null) {
                    Map<String, Object> emailData = (Map<String, Object>) response.getData();
                    email = (String) emailData.get("email");
                }
            } catch (Exception e) {
                log.error("Failed to fetch email for user {}: {}", member.getUserId(), e.getMessage());
            }

            boolean isKid = parentControlRepository.existsByKidId(member.getUserId());

            FamilyMemberDetailResponse detailResponse = FamilyMemberDetailResponse.builder()
                    .id(member.getId())
                    .userId(member.getUserId())
                    .email(email)
                    .isOwner(member.getIsOwner())
                    .isKid(isKid)
                    .joinedAt(member.getJoinedAt())
                    .build();

            responses.add(detailResponse);
        }

        return responses;
    }
}
