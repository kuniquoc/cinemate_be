package com.pbl6.cinemate.movie.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ActorResponse(
        UUID id,
        String fullname,
        String biography,
        String avatar,
        LocalDate dateOfBirth,
        Instant createdAt,
        Instant updatedAt) {
}
