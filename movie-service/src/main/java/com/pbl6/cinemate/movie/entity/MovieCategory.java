package com.pbl6.cinemate.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "movie_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}