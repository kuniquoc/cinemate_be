package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.ParentControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentControlRepository extends JpaRepository<ParentControl, UUID> {
    
    Optional<ParentControl> findByParentIdAndKidId(UUID parentId, UUID kidId);
    
    List<ParentControl> findByParentId(UUID parentId);
    
    List<ParentControl> findByKidId(UUID kidId);
    
    Optional<ParentControl> findByKidIdAndSubscriptionId(UUID kidId, UUID subscriptionId);
    
    boolean existsByParentIdAndKidId(UUID parentId, UUID kidId);
}
