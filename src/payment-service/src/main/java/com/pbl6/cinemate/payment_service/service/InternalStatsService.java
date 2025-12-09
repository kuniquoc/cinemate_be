package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.response.PaymentStatsResponse;

/**
 * Service for internal statistics
 */
public interface InternalStatsService {

    /**
     * Get payment and subscription statistics
     * 
     * @return PaymentStatsResponse containing activeSubscriptions, totalRevenue,
     *         ordersToday
     */
    PaymentStatsResponse getPaymentStats();
}
