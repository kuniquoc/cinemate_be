package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {
    
    List<FamilyMember> findBySubscriptionId(UUID subscriptionId);
    
    Optional<FamilyMember> findBySubscriptionIdAndUserId(UUID subscriptionId, UUID userId);
    
    Optional<FamilyMember> findBySubscriptionIdAndIsOwnerTrue(UUID subscriptionId);
    
    Long countBySubscriptionId(UUID subscriptionId);
    
    boolean existsBySubscriptionIdAndUserId(UUID subscriptionId, UUID userId);
    
    List<FamilyMember> findByUserId(UUID userId);
}
