package com.pbl6.cinemate.payment_service.email;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.shared.email.EmailService;
import com.pbl6.cinemate.shared.email.dto.TemplateEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyInvitationEmailService {

    private final EmailService emailService;

    /**
     * Send invitation email to a family member
     * 
     * @param inviterEmail   Email of the person sending the invitation
     * @param recipientEmail Email address of the recipient
     * @param invitationLink Full invitation link to accept
     * @param mode           ADULT or KID mode
     * @param expiresAt      Expiration date of the invitation
     */
    public void sendInvitationEmail(
            String inviterEmail,
            String recipientEmail,
            String invitationLink,
            InvitationMode mode,
            Instant expiresAt) {

        try {
            String templateName;
            String subject;
            Map<String, Object> variables = new HashMap<>();

            // Common variables
            variables.put("invitationLink", invitationLink);
            variables.put("expiresAt", expiresAt);
            variables.put("recipientEmail", recipientEmail);

            // Choose template and subject based on mode
            if (mode == InvitationMode.KID) {
                templateName = "email/family-invitation-kid";
                subject = "You're Invited to Join Cinemate Family Plan! 🎬";
                variables.put("parentName", inviterEmail);
            } else {
                templateName = "email/family-invitation-adult";
                subject = "Join " + inviterEmail + "'s Cinemate Family Plan";
                variables.put("inviterName", inviterEmail);
            }

            // Build and send email request
            TemplateEmailRequest emailRequest = TemplateEmailRequest.builder()
                    .to(recipientEmail)
                    .subject(subject)
                    .templateName(templateName)
                    .variables(variables)
                    .locale(Locale.ENGLISH)
                    .build();

            emailService.sendTemplateEmail(emailRequest);

            log.info("Family invitation email sent to {} (mode: {})", recipientEmail, mode);

        } catch (Exception e) {
            log.error("Failed to send family invitation email to {}: {}", recipientEmail, e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break the invitation creation
        }
    }
}
