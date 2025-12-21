package com.pbl6.cinemate.movie.client.dto;

import java.math.BigDecimal;

/**
 * Response from payment-service internal API for payment stats
 */
public record PaymentStatsResponse(
        long activeSubscriptions,
        BigDecimal totalRevenue,
        long ordersToday) {
}