package com.pbl6.cinemate.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.pbl6.cinemate.shared.utils.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base entity class for all JPA entities.
 * Provides:
 * - UUID v7 primary key generated at application layer
 * - Audit fields (createdAt, updatedAt, deletedAt)
 * - Support for manual ID assignment (for seeders)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AbstractBaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Called before persisting a new entity.
     * Generates UUID v7 if not already set (allows manual ID assignment for
     * seeders).
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidGenerator.generateV7();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        // Ensure updatedAt is initialized on insert so DB NOT NULL/defaults aren't
        // bypassed
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    /**
     * Called before updating an entity.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Soft delete the entity by setting deletedAt timestamp.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Check if the entity is soft deleted.
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
