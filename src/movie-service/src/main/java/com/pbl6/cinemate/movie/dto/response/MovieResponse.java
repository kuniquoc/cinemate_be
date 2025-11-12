package com.pbl6.cinemate.movie.dto.response;

import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        String description,
        String horizontalPoster,
        String verticalPoster,
        String categoryName,
        Boolean isVip,
        Integer age,
        Integer year,
        String trailerUrl
) {
}
