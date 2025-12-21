package com.pbl6.cinemate.movie.entity;

import com.pbl6.cinemate.movie.enums.MovieProcessStatus;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Movie extends AbstractBaseEntity {

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    @Enumerated(EnumType.STRING)
    private MovieProcessStatus processStatus;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> qualities;

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

    @Column(name = "rank")
    private Integer rank;

    // Duration in seconds (nullable)
    private Long duration;

    @Builder.Default
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MovieCategory> movieCategories = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MovieActor> movieActors = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MovieDirector> movieDirectors = new HashSet<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = MovieStatus.DRAFT;
        }
        if (this.processStatus == null) {
            this.processStatus = MovieProcessStatus.UPLOADING;
        }
        if (this.isVip == null) {
            this.isVip = false;
        }
    }
}
