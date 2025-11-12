package com.pbl6.cinemate.movie.entity;

import com.pbl6.cinemate.movie.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String qualitiesJson;

    // New fields
    private String verticalPoster;
    private String horizontalPoster;
    private LocalDate releaseDate;
    private String trailerUrl;
    private Integer age;
    private Integer year;
    private String country;
    
    @Column(name = "is_vip", nullable = false)
    private Boolean isVip;

    public Movie(String title, String description, MovieStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.status = MovieStatus.PENDING;
        if (this.isVip == null) {
            this.isVip = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

}
