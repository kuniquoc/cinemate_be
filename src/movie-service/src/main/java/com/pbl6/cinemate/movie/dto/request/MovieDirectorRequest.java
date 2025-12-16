package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record MovieDirectorRequest(
        @NotEmpty(message = "Director IDs cannot be empty")
        List<UUID> directorIds
) {
}
