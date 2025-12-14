package com.pbl6.cinemate.movie.dto.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

public record ActorCreationRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullname,

        @Size(max = 2000, message = "Biography must not exceed 2000 characters")
        String biography,

        @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
        String avatar,

        @Past(message = "Date of birth must be in the past")
        @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
        LocalDate dateOfBirth
) {
}
