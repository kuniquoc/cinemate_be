package com.pbl6.cinemate.movie.entity;

import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "director")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Director extends AbstractBaseEntity {

    @Column(nullable = false)
    private String fullname;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @Column(length = 512)
    private String avatar;

    private LocalDate dateOfBirth;

    @Builder.Default
    @OneToMany(mappedBy = "director", fetch = FetchType.LAZY)
    private Set<MovieDirector> movieDirectors = new HashSet<>();
}
