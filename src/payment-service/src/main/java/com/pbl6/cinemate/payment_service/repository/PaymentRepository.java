package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);

    List<Payment> findBySubscriptionId(UUID subscriptionId);

    /**
     * Sum total revenue from successful payments
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    /**
     * Count successful payments within date range (orders today)
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :startDate AND p.createdAt < :endDate")
    long countByStatusAndCreatedAtBetween(
            @Param("status") PaymentStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);
}
