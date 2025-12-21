package com.pbl6.cinemate.movie.client;

import com.pbl6.cinemate.movie.client.dto.PaymentStatsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Fallback implementation for PaymentServiceClient
 */
@Slf4j
@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public PaymentStatsResponse getPaymentStats() {
        log.warn("Fallback: Failed to get payment stats from payment-service");
        return new PaymentStatsResponse(0L, BigDecimal.ZERO, 0L);
    }
}