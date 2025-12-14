package com.pbl6.cinemate.movie.entity;

import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "watch_history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"movie_id", "customer_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory extends AbstractBaseEntity {

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

    @Override
    protected void onCreate() {
        super.onCreate();
        calculateProgressPercent();
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
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
