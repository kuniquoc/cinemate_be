package com.pbl6.cinemate.payment_service.dto.request;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateInvitationRequest {
    
    @NotNull(message = "Invitation mode is required")
    private InvitationMode mode;
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;
    
    private Boolean sendEmail = true; // Default to true
}
