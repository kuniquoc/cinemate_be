package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a review
 * User info (userId, userName, userAvatar) is now obtained from JWT and
 * customer-service
 */
public record ReviewCreationRequest(
        @NotBlank(message = "Content is required") @Size(max = 2000, message = "Content must not exceed 2000 characters") String content,

        @NotNull(message = "Stars rating is required") @Min(value = 1, message = "Stars rating must be at least 1") @Max(value = 5, message = "Stars rating must not exceed 5") Integer stars) {
}
