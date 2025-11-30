package com.pbl6.cinemate.payment_service.dto.request;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateInvitationRequest {
    
    @NotNull(message = "Subscription ID is required")
    private UUID subscriptionId;
    
    @NotNull(message = "Invitation mode is required")
    private InvitationMode mode;
}
