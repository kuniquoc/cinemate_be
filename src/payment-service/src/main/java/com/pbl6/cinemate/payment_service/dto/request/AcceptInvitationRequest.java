package com.pbl6.cinemate.payment_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptInvitationRequest {
    
    @NotBlank(message = "Invitation token is required")
    private String invitationToken;
}
