package com.pbl6.cinemate.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for payment statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsResponse {
    private long activeSubscriptions;
    private BigDecimal totalRevenue;
    private long ordersToday;
}