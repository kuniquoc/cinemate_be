package com.pbl6.cinemate.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "actor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Actor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullname;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(length = 512)
    private String avatar;

    private LocalDate dateOfBirth;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Actor(String fullname, String biography, String avatar, LocalDate dateOfBirth) {
        this.fullname = fullname;
        this.biography = biography;
        this.avatar = avatar;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}