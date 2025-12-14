package com.pbl6.cinemate.payment_service.entity;

import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "parent_control")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ParentControl extends AbstractBaseEntity {

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "kid_id", nullable = false)
    private UUID kidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "blocked_categories", columnDefinition = "TEXT")
    private String blockedCategories; // Comma-separated list of category UUIDs to block

    @Column(name = "watch_time_limit_minutes")
    private Integer watchTimeLimitMinutes;
}
