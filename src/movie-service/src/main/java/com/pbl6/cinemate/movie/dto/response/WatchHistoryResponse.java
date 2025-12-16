package com.pbl6.cinemate.movie.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WatchHistoryResponse(
        UUID id,
        String title,
        String description,
        String status,
        String horizontalPoster,
        String verticalPoster,
        LocalDate releaseDate,
        String trailerUrl,
        Integer age,
        Integer year,
        String country,
        Boolean isVip,
        Integer rank,
        List<CategoryResponse> categories,
        List<ActorResponse> actors,
        List<DirectorResponse> directors,
        Long lastWatchedPosition,
        Long totalDuration,
        Double progressPercent,
        Instant watchedAt) {
}
