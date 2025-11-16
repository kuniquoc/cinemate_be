package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    
    Optional<Payment> findByTransactionId(String transactionId);
    
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
    
    List<Payment> findBySubscriptionId(Long subscriptionId);
}
