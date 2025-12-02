package com.pbl6.cinemate.payment_service.dto.response;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInvitationResponse {
    private UUID id;
    private String invitationToken;
    private String invitationLink;
    private InvitationMode mode;
    private InvitationStatus status;
    private UUID invitedBy;
    private UUID invitedUserId;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
}
