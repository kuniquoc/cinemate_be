package com.pbl6.cinemate.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.payment_service.client.MovieServiceClient;
import com.pbl6.cinemate.payment_service.dto.CategoryDto;
import com.pbl6.cinemate.payment_service.dto.request.AcceptInvitationRequest;
import com.pbl6.cinemate.payment_service.dto.request.CreateInvitationRequest;
import com.pbl6.cinemate.payment_service.dto.request.UpdateParentControlRequest;
import com.pbl6.cinemate.payment_service.dto.response.FamilyInvitationResponse;
import com.pbl6.cinemate.payment_service.dto.response.FamilyMemberDetailResponse;
import com.pbl6.cinemate.payment_service.dto.response.FamilyMemberResponse;
import com.pbl6.cinemate.payment_service.dto.response.ParentControlResponse;
import com.pbl6.cinemate.payment_service.entity.FamilyInvitation;
import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import com.pbl6.cinemate.payment_service.entity.ParentControl;
import com.pbl6.cinemate.payment_service.service.FamilyPlanService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/family-plans")
@RequiredArgsConstructor
public class FamilyPlanController {

        private final FamilyPlanService familyPlanService;
        private final ModelMapper modelMapper;
        private final MovieServiceClient movieServiceClient;
        private final ObjectMapper objectMapper;

        @Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

        @PostMapping("/invitations")
        public ResponseEntity<ResponseData> createInvitation(
                        @Valid @RequestBody CreateInvitationRequest request,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                FamilyInvitation invitation = familyPlanService.createInvitation(
                                userPrincipal.getId(),
                                request.getMode(),
                                userPrincipal.getUsername(),
                                request.getRecipientEmail(),
                                request.getSendEmail());

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
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                FamilyMember member = familyPlanService.acceptInvitation(
                                request.getInvitationToken(),
                                userPrincipal.getId());

                FamilyMemberResponse response = modelMapper.map(member, FamilyMemberResponse.class);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Invitation accepted successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/members")
        public ResponseEntity<ResponseData> getMyFamilyMembers(
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                List<FamilyMemberDetailResponse> members = familyPlanService
                                .getCurrentUserFamilyMembers(userPrincipal.getId());

                return ResponseEntity.ok(ResponseData.success(
                                members,
                                "Family members retrieved successfully",
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

        @DeleteMapping("/members/{memberUserId}")
        public ResponseEntity<ResponseData> removeMember(
                        @PathVariable UUID memberUserId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                familyPlanService.removeMember(userPrincipal.getId(), memberUserId);

                return ResponseEntity.ok(ResponseData.success(
                                "Member removed successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @DeleteMapping("/invitations/{invitationId}")
        public ResponseEntity<ResponseData> cancelInvitation(
                        @PathVariable UUID invitationId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                familyPlanService.cancelInvitation(invitationId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseData.success(
                                "Invitation cancelled successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/parent-control")
        public ResponseEntity<ResponseData> getParentControl(
                        @CurrentUser UserPrincipal userPrincipal,
                        @RequestParam(name = "kidId") UUID kidId,
                        HttpServletRequest httpRequest) {

                ParentControl control = familyPlanService.getParentControl(userPrincipal.getId(), kidId);
                ParentControlResponse response = toParentControlResponse(control);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Parent control retrieved successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @PutMapping("/parent-control")
        public ResponseEntity<ResponseData> updateParentControl(
                        @CurrentUser UserPrincipal userPrincipal,
                        @RequestParam(name = "kidId") UUID kidId,
                        @Valid @RequestBody UpdateParentControlRequest request,
                        HttpServletRequest httpRequest) {

                ParentControl control = familyPlanService.updateParentControl(
                                userPrincipal.getId(),
                                kidId,
                                request.getBlockedCategoryIds(),
                                request.getWatchTimeLimitMinutes());

                ParentControlResponse response = toParentControlResponse(control);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Parent control updated successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/parent-control/kids")
        public ResponseEntity<ResponseData> getKidsForParent(
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {

                List<ParentControl> controls = familyPlanService.getKidsForParent(userPrincipal.getId());
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
                // Manually build response instead of using ModelMapper to avoid field type
                // conflicts
                ParentControlResponse response = new ParentControlResponse();
                response.setId(control.getId());
                response.setParentId(control.getParentId());
                response.setKidId(control.getKidId());
                response.setSubscriptionId(control.getSubscription().getId());
                response.setWatchTimeLimitMinutes(control.getWatchTimeLimitMinutes());
                response.setCreatedAt(control.getCreatedAt());
                response.setUpdatedAt(control.getUpdatedAt());

                // Convert comma-separated UUID string to List<CategoryDto>
                List<CategoryDto> categoryDtos = new ArrayList<>();
                String blockedCats = control.getBlockedCategories();
                log.info("toParentControlResponse - blockedCategories from DB: '{}'", blockedCats);

                if (blockedCats != null && !blockedCats.isEmpty()) {
                        String[] categoryIdStrings = blockedCats.split(",");
                        log.info("Split into {} category IDs", categoryIdStrings.length);
                        for (String idString : categoryIdStrings) {
                                try {
                                        UUID categoryId = UUID.fromString(idString.trim());

                                        // Fetch from movie-service which returns ResponseData wrapper
                                        ResponseData responseData = movieServiceClient.getCategoryById(categoryId);

                                        // Extract the category data from the wrapper
                                        if (responseData != null && responseData.getData() != null) {
                                                // Convert the data (LinkedHashMap) to CategoryDto
                                                Map<String, Object> dataMap = (Map<String, Object>) responseData
                                                                .getData();
                                                CategoryDto categoryDto = CategoryDto.builder()
                                                                .id(UUID.fromString(dataMap.get("id").toString()))
                                                                .name((String) dataMap.get("name"))
                                                                .build();
                                                categoryDtos.add(categoryDto);
                                                log.info("Successfully added category: {}", categoryDto.getName());
                                        } else {
                                                log.warn("ResponseData or data is null for category {}", categoryId);
                                        }
                                } catch (Exception e) {
                                        log.error("Failed to fetch category {}: {}", idString, e.getMessage(), e);
                                }
                        }
                } else {
                        log.warn("blockedCategories is null or empty!");
                }
                response.setBlockedCategories(categoryDtos);
                return response;
        }
}
