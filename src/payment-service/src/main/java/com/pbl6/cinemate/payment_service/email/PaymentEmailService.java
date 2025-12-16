package com.pbl6.cinemate.payment_service.email;

import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.entity.SubscriptionPlan;
import com.pbl6.cinemate.shared.email.EmailService;
import com.pbl6.cinemate.shared.email.dto.TemplateEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEmailService {

    private final EmailService emailService;

    /**
     * Send payment success confirmation email
     *
     * @param recipientEmail Recipient's email address
     * @param recipientName  Recipient's name
     * @param payment        Payment entity with transaction details
     * @param plan           Subscription plan details
     */
    public void sendPaymentSuccessEmail(
            String recipientEmail,
            String recipientName,
            Payment payment,
            SubscriptionPlan plan) {

        try {
            Map<String, Object> variables = new HashMap<>();

            // Recipient info
            variables.put("recipientName", recipientName);
            variables.put("recipientEmail", recipientEmail);

            // Payment details
            variables.put("transactionId", payment.getVnpTransactionNo());
            variables.put("amount", payment.getAmount());
            variables.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate() : Instant.now());
            variables.put("paymentMethod", payment.getPaymentMethod().toString());

            // Plan details
            variables.put("planName", plan.getName());
            variables.put("planPrice", plan.getPrice());
            variables.put("planDuration", plan.getDurationDays());

            // Build and send email request
            TemplateEmailRequest emailRequest = TemplateEmailRequest.builder()
                    .to(recipientEmail)
                    .subject("Payment Successful - Cinemate " + plan.getName() + " Plan")
                    .templateName("email/payment-success")
                    .variables(variables)
                    .locale(Locale.ENGLISH)
                    .build();

            emailService.sendTemplateEmail(emailRequest);

            log.info("Payment success email sent to {} for transaction {}",
                    recipientEmail, payment.getVnpTransactionNo());

        } catch (Exception e) {
            log.error("Failed to send payment success email to {}: {}",
                    recipientEmail, e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break the payment flow
        }
    }

    /**
     * Send subscription activated email
     *
     * @param recipientEmail Recipient's email address
     * @param recipientName  Recipient's name
     * @param subscription   Activated subscription entity
     */
    public void sendSubscriptionActivatedEmail(
            String recipientEmail,
            String recipientName,
            Subscription subscription) {

        try {
            SubscriptionPlan plan = subscription.getPlan();

            Map<String, Object> variables = new HashMap<>();

            // Recipient info
            variables.put("recipientName", recipientName);
            variables.put("recipientEmail", recipientEmail);

            // Subscription details
            variables.put("planName", plan.getName());
            variables.put("startDate", subscription.getStartDate());
            variables.put("endDate", subscription.getEndDate());
            variables.put("autoRenew", subscription.getAutoRenew());

            // Plan features
            variables.put("isFamilyPlan", plan.getIsFamilyPlan());
            variables.put("maxDevices", plan.getMaxDevices());

            if (plan.getIsFamilyPlan()) {
                variables.put("maxMembers", plan.getMaxMembers());
            }

            // Build and send email request
            TemplateEmailRequest emailRequest = TemplateEmailRequest.builder()
                    .to(recipientEmail)
                    .subject("Welcome to Cinemate " + plan.getName() + " Plan!")
                    .templateName("email/subscription-activated")
                    .variables(variables)
                    .locale(Locale.ENGLISH)
                    .build();

            emailService.sendTemplateEmail(emailRequest);

            log.info("Subscription activated email sent to {} for subscription {}",
                    recipientEmail, subscription.getId());

        } catch (Exception e) {
            log.error("Failed to send subscription activated email to {}: {}",
                    recipientEmail, e.getMessage(), e);
            // Don't throw exception - email failure shouldn't break the activation flow
        }
    }
}
