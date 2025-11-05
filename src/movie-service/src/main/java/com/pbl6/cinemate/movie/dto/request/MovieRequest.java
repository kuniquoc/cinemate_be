package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Description is required")
    private String description;
    @NotBlank(message = "Horizontal poster URL is required")
    private String horizontalPoster;
    @NotBlank(message = "Vertical poster URL is required")
    private String verticalPoster;
    @NotNull(message = "release date is required")
    private LocalDate releaseDate;
    @NotBlank(message = "trailer URL is required")
    private String trailerUrl;
    @NotNull(message = "age is required")
    private Integer age;
    @NotNull(message = "year is required")
    private Integer year;
    @NotBlank(message = "country is required")
    private String country;
}
