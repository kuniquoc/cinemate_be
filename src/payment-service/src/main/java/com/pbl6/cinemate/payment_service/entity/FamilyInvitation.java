package com.pbl6.cinemate.payment_service.entity;

import com.pbl6.cinemate.payment_service.enums.InvitationMode;
import com.pbl6.cinemate.payment_service.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInvitation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
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
    private LocalDateTime expiresAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(7); // Default 7 days expiration
        }
    }
}
