package com.pbl6.cinemate.movie.dto.response;


import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record MovieInfoResponse(UUID id, String title, String description, String status,
                                Map<String, String> qualities, String verticalPoster, String horizontalPoster, LocalDate releaseDate,
                                String trailerUrl, Integer age, Integer year, String country) {
}
