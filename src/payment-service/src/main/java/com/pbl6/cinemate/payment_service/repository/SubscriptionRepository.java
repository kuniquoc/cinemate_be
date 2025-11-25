package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Subscription;
import com.pbl6.cinemate.payment_service.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    
    List<Subscription> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.endDate < :now")
    List<Subscription> findExpiredSubscriptions(@Param("status") SubscriptionStatus status, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
