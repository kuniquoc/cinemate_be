package com.pbl6.cinemate.movie.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MovieInfoResponse(
        UUID id,
        String title,
        String description,
        String status,
        List<String> qualities,
        String verticalPoster,
        String horizontalPoster,
        LocalDate releaseDate,
        String trailerUrl,
        Integer age,
        Integer year,
        String country,
        Boolean isVip,
        Integer rank,
        List<ActorResponse> actors,
        List<DirectorResponse> directors,
        List<CategoryResponse> categories,
        Long lastWatchedPosition) {
}
