package com.pbl6.cinemate.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watch_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "movie_id", "customer_id" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "last_watched_position", nullable = false)
    private Long lastWatchedPosition; // in seconds

    @Column(name = "total_duration", nullable = false)
    private Long totalDuration; // in seconds

    @Column(name = "progress_percent", nullable = false)
    private Double progressPercent;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        calculateProgressPercent();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        calculateProgressPercent();
    }

    private void calculateProgressPercent() {
        if (this.totalDuration != null && this.totalDuration > 0 && this.lastWatchedPosition != null) {
            this.progressPercent = (double) this.lastWatchedPosition / this.totalDuration * 100;
        } else {
            this.progressPercent = 0.0;
        }
    }
}
