package com.pbl6.cinemate.movie.dto.response;

import java.util.List;
import java.util.UUID;

public record WatchHistoryResponse(
        UUID id,
        String title,
        String description,
        List<String> qualities,
        String horizontalPoster,
        int age,
        int year,
        List<CategoryResponse> categories,
        long lastWatchedPosition,
        long totalDuration,
        double progressPercent) {
}
