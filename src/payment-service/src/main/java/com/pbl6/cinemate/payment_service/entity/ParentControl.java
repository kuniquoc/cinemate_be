package com.pbl6.cinemate.payment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parent_control")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentControl {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
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
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
