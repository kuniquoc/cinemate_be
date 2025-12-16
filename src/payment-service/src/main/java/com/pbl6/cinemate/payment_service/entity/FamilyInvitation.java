package com.pbl6.cinemate.payment_service.entity;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_invitations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInvitation extends AbstractBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    private InvitationMode mode; // ADULT or KID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status; // PENDING, ACCEPTED, EXPIRED, CANCELLED

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "invited_user_id")
    private UUID invitedUserId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (expiresAt == null) {
            expiresAt = Instant.now().plus(Duration.ofDays(7)); // Default 7 days expiration
        }
    }
}
