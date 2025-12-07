package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.AcceptInvitationRequest;
import com.pbl6.cinemate.payment_service.dto.request.CreateInvitationRequest;
import com.pbl6.cinemate.payment_service.dto.request.UpdateParentControlRequest;
import com.pbl6.cinemate.payment_service.dto.response.FamilyInvitationResponse;
import com.pbl6.cinemate.payment_service.dto.response.FamilyMemberResponse;
import com.pbl6.cinemate.payment_service.dto.response.ParentControlResponse;
import com.pbl6.cinemate.payment_service.entity.FamilyInvitation;
import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import com.pbl6.cinemate.payment_service.entity.ParentControl;
import com.pbl6.cinemate.payment_service.service.FamilyPlanService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/family-plans")
@RequiredArgsConstructor
public class FamilyPlanController {
    
    private final FamilyPlanService familyPlanService;
    private final ModelMapper modelMapper;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @PostMapping("/invitations")
    public ResponseEntity<ResponseData> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @RequestParam(name = "userId") UUID userId,
            HttpServletRequest httpRequest) {
        
        FamilyInvitation invitation = familyPlanService.createInvitation(
                request.getSubscriptionId(),
                userId,
                request.getMode(),
                request.getInviterName(),
                request.getRecipientEmail(),
                request.getSendEmail()
        );
        
        FamilyInvitationResponse response = toInvitationResponse(invitation);
        
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Invitation created successfully" + (request.getSendEmail() ? " and email sent" : ""),
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @PostMapping("/invitations/accept")
    public ResponseEntity<ResponseData> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            @RequestParam(name = "userId") UUID userId,
            HttpServletRequest httpRequest) {
        
        FamilyMember member = familyPlanService.acceptInvitation(
                request.getInvitationToken(),
                userId
        );
        
        FamilyMemberResponse response = modelMapper.map(member, FamilyMemberResponse.class);
        
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Invitation accepted successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/subscriptions/{subscriptionId}/members")
    public ResponseEntity<ResponseData> getFamilyMembers(
            @PathVariable UUID subscriptionId,
            HttpServletRequest httpRequest) {
        
        List<FamilyMember> members = familyPlanService.getFamilyMembers(subscriptionId);
        List<FamilyMemberResponse> responses = members.stream()
                .map(m -> modelMapper.map(m, FamilyMemberResponse.class))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ResponseData.success(
                responses,
                "Family members retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/subscriptions/{subscriptionId}/invitations")
    public ResponseEntity<ResponseData> getInvitations(
            @PathVariable UUID subscriptionId,
            HttpServletRequest httpRequest) {
        
        List<FamilyInvitation> invitations = familyPlanService.getInvitations(subscriptionId);
        List<FamilyInvitationResponse> responses = invitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ResponseData.success(
                responses,
                "Invitations retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @DeleteMapping("/subscriptions/{subscriptionId}/members/{memberUserId}")
    public ResponseEntity<ResponseData> removeMember(
            @PathVariable UUID subscriptionId,
            @PathVariable UUID memberUserId,
            @RequestParam(name = "ownerId") UUID ownerId,
            HttpServletRequest httpRequest) {
        
        familyPlanService.removeMember(subscriptionId, ownerId, memberUserId);
        
        return ResponseEntity.ok(ResponseData.success(
                "Member removed successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<ResponseData> cancelInvitation(
            @PathVariable UUID invitationId,
            @RequestParam(name = "userId") UUID userId,
            HttpServletRequest httpRequest) {
        
        familyPlanService.cancelInvitation(invitationId, userId);
        
        return ResponseEntity.ok(ResponseData.success(
                "Invitation cancelled successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/parent-control")
    public ResponseEntity<ResponseData> getParentControl(
            @RequestParam(name = "parentId") UUID parentId,
            @RequestParam(name = "kidId") UUID kidId,
            HttpServletRequest httpRequest) {
        
        ParentControl control = familyPlanService.getParentControl(parentId, kidId);
        ParentControlResponse response = toParentControlResponse(control);
        
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Parent control retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @PutMapping("/parent-control")
    public ResponseEntity<ResponseData> updateParentControl(
            @RequestParam(name = "parentId") UUID parentId,
            @RequestParam(name = "kidId") UUID kidId,
            @Valid @RequestBody UpdateParentControlRequest request,
            HttpServletRequest httpRequest) {
        
        ParentControl control = familyPlanService.updateParentControl(
                parentId,
                kidId,
                request.getBlockedCategories(),
                request.getWatchTimeLimitMinutes()
        );
        
        ParentControlResponse response = toParentControlResponse(control);
        
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Parent control updated successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    @GetMapping("/parent-control/kids")
    public ResponseEntity<ResponseData> getKidsForParent(
            @RequestParam(name = "parentId") UUID parentId,
            HttpServletRequest httpRequest) {
        
        List<ParentControl> controls = familyPlanService.getKidsForParent(parentId);
        List<ParentControlResponse> responses = controls.stream()
                .map(this::toParentControlResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ResponseData.success(
                responses,
                "Kids retrieved successfully",
                httpRequest.getRequestURI(),
                httpRequest.getMethod()));
    }
    
    private FamilyInvitationResponse toInvitationResponse(FamilyInvitation invitation) {
        FamilyInvitationResponse response = modelMapper.map(invitation, FamilyInvitationResponse.class);
        response.setInvitationLink(frontendUrl + "/family/join?token=" + invitation.getInvitationToken());
        return response;
    }
    
    private ParentControlResponse toParentControlResponse(ParentControl control) {
        ParentControlResponse response = modelMapper.map(control, ParentControlResponse.class);
        // Convert comma-separated string to list
        if (control.getBlockedCategories() != null && !control.getBlockedCategories().isEmpty()) {
            response.setBlockedCategories(List.of(control.getBlockedCategories().split(",")));
        } else {
            response.setBlockedCategories(List.of());
        }
        return response;
    }
}
