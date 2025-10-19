package com.pbl6.cinemate.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.pbl6.cinemate.movie.enums.MovieStatus;

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

    public Movie(String title, String description, MovieStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

}
