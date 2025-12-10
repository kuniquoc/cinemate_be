package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating a review
 * User ID is now obtained from JWT via @CurrentUser
 */
public record ReviewUpdateRequest(
        @Size(max = 2000, message = "Content must not exceed 2000 characters") String content,

        @Min(value = 1, message = "Stars rating must be at least 1") @Max(value = 5, message = "Stars rating must not exceed 5") Integer stars) {
}
