package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
