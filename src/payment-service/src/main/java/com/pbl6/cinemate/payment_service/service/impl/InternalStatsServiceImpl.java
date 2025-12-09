package com.pbl6.cinemate.payment_service.service.impl;

import com.pbl6.cinemate.payment_service.dto.response.PaymentStatsResponse;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import com.pbl6.cinemate.payment_service.enums.SubscriptionStatus;
import com.pbl6.cinemate.payment_service.repository.PaymentRepository;
import com.pbl6.cinemate.payment_service.repository.SubscriptionRepository;
import com.pbl6.cinemate.payment_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InternalStatsServiceImpl implements InternalStatsService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentStatsResponse getPaymentStats() {
        // Count active subscriptions
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        // Sum total revenue from successful payments
        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);

        // Count orders today (successful payments today)
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long ordersToday = paymentRepository.countByStatusAndCreatedAtBetween(
                PaymentStatus.SUCCESS, todayStart, todayEnd);

        return PaymentStatsResponse.builder()
                .activeSubscriptions(activeSubscriptions)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .ordersToday(ordersToday)
                .build();
    }
}
