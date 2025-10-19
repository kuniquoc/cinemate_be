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
@Table(name = "review")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer stars;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_avatar", length = 512)
    private String userAvatar;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Review(Movie movie, UUID customerId, String content, Integer stars, String userName, String userAvatar) {
        this.movie = movie;
        this.customerId = customerId;
        this.content = content;
        this.stars = stars;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
