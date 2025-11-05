package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record ReviewCreationRequest(
    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    String content,

    @NotNull(message = "Stars rating is required")
    @Min(value = 1, message = "Stars rating must be at least 1")
    @Max(value = 5, message = "Stars rating must not exceed 5")
    Integer stars,

    @NotBlank(message = "User name is required")
    @Size(max = 255, message = "User name must not exceed 255 characters")
    String userName,

    @NotBlank(message = "User avatar URL is required")
    @Size(max = 512, message = "User avatar URL must not exceed 512 characters")
    String userAvatar,

    @NotNull(message = "User ID is required")
    UUID userId
) {
}
