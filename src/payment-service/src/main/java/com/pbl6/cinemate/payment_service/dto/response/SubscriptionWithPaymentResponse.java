package com.pbl6.cinemate.payment_service.dto.response;

import com.pbl6.cinemate.payment_service.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Combined response containing both subscription and payment details
 * Returned when creating a new subscription with auto-generated payment URL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionWithPaymentResponse {

    // Subscription details
    private UUID subscriptionId;
    private UUID userId;
    private SubscriptionPlanResponse plan;
    private SubscriptionStatus status;
    private Boolean autoRenew;
    private Instant createdAt;

    // Payment details
    private UUID paymentId;
    private String paymentUrl;
    private String vnpTxnRef;
    private BigDecimal amount;
    private String message;
}
