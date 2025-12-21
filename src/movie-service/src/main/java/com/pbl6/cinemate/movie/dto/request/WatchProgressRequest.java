package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WatchProgressRequest(
                @NotNull(message = "Last watched position is required") @Min(value = 0, message = "Last watched position must be at least 0") Long lastWatchedPosition) {
}
