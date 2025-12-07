package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.email.FamilyInvitationEmailService;
import com.pbl6.cinemate.payment_service.entity.*;
import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import com.pbl6.cinemate.payment_service.exception.DeviceLimitException;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.exception.SubscriptionException;
import com.pbl6.cinemate.payment_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyPlanService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository invitationRepository;
    private final ParentControlRepository parentControlRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final FamilyInvitationEmailService emailService;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Transactional
    public FamilyInvitation createInvitation(
            UUID subscriptionId, 
            UUID ownerId, 
            InvitationMode mode,
            String inviterName,
            String recipientEmail,
            Boolean sendEmail) {
        
        // Verify subscription exists and is family plan
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        if (!subscription.getPlan().getIsFamilyPlan()) {
            throw new SubscriptionException("This is not a family plan subscription");
        }
        
        // Verify user is the owner
        FamilyMember owner = familyMemberRepository.findBySubscriptionIdAndUserId(subscriptionId, ownerId)
                .orElseThrow(() -> new SubscriptionException("User is not a member of this subscription"));
        
        if (!owner.getIsOwner()) {
            throw new SubscriptionException("Only the plan owner can create invitations");
        }
        
        // Check if subscription has available slots
        Long currentMembers = familyMemberRepository.countBySubscriptionId(subscriptionId);
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
        invitation.setInvitedBy(ownerId);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        
        FamilyInvitation savedInvitation = invitationRepository.save(invitation);
        
        // Send email if requested
        if (sendEmail != null && sendEmail && recipientEmail != null && !recipientEmail.isEmpty()) {
            String invitationLink = frontendUrl + "/family/join?token=" + token;
            emailService.sendInvitationEmail(
                    inviterName,
                    recipientEmail,
                    invitationLink,
                    mode,
                    savedInvitation.getExpiresAt()
            );
        }
        
        return savedInvitation;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public FamilyInvitation createInvitation(UUID subscriptionId, UUID ownerId, InvitationMode mode) {
        return createInvitation(subscriptionId, ownerId, mode, null, null, false);
    }
    
    @Transactional
    public FamilyMember acceptInvitation(String token, UUID userId) {
        // Find invitation
        FamilyInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        // Validate invitation
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new SubscriptionException("Invitation is no longer valid");
        }
        
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new SubscriptionException("Invitation has expired");
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
        member.setJoinedAt(LocalDateTime.now());
        
        FamilyMember savedMember = familyMemberRepository.save(member);
        
        // If KID mode, create parent control record
        if (invitation.getMode() == InvitationMode.KID) {
            ParentControl parentControl = new ParentControl();
            parentControl.setParentId(invitation.getInvitedBy());
            parentControl.setKidId(userId);
            parentControl.setSubscription(subscription);
            parentControl.setBlockedCategories("Horror,Thriller,Adult"); // Default blocked categories
            parentControlRepository.save(parentControl);
        }
        
        // Update invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setInvitedUserId(userId);
        invitation.setAcceptedAt(LocalDateTime.now());
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
    public void removeMember(UUID subscriptionId, UUID ownerId, UUID memberUserId) {
        // Verify owner
        FamilyMember owner = familyMemberRepository.findBySubscriptionIdAndUserId(subscriptionId, ownerId)
                .orElseThrow(() -> new SubscriptionException("User is not a member of this subscription"));
        
        if (!owner.getIsOwner()) {
            throw new SubscriptionException("Only the plan owner can remove members");
        }
        
        // Find and remove member
        FamilyMember member = familyMemberRepository.findBySubscriptionIdAndUserId(subscriptionId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        
        if (member.getIsOwner()) {
            throw new SubscriptionException("Cannot remove the plan owner");
        }
        
        // Remove parent control records if exists
        parentControlRepository.findByKidIdAndSubscriptionId(memberUserId, subscriptionId)
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
    public ParentControl updateParentControl(UUID parentId, UUID kidId, List<String> blockedCategories, Integer watchTimeLimit) {
        ParentControl control = parentControlRepository.findByParentIdAndKidId(parentId, kidId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent control not found"));
        
        if (blockedCategories != null) {
            control.setBlockedCategories(String.join(",", blockedCategories));
        }
        
        if (watchTimeLimit != null) {
            control.setWatchTimeLimitMinutes(watchTimeLimit);
        }
        
        return parentControlRepository.save(control);
    }
    
    public List<ParentControl> getKidsForParent(UUID parentId) {
        return parentControlRepository.findByParentId(parentId);
    }
}
