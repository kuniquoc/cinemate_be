package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.FamilyInvitation;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {

    Optional<FamilyInvitation> findByInvitationToken(String invitationToken);

    List<FamilyInvitation> findBySubscriptionId(UUID subscriptionId);

    List<FamilyInvitation> findBySubscriptionIdAndStatus(UUID subscriptionId, InvitationStatus status);

    List<FamilyInvitation> findByInvitedBy(UUID invitedBy);
}
