package com.pbl6.cinemate.movie.dto.request;

import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.shared.validation.ValidEnum;
import jakarta.validation.constraints.NotNull;

public record UpdateMovieStatusRequest(
        @NotNull(message = "Status is required") @ValidEnum(enumClass = MovieStatus.class, message = "Invalid status value") String status) {
}
