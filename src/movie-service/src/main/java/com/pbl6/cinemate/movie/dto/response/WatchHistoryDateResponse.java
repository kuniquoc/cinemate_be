package com.pbl6.cinemate.movie.dto.response;

import java.time.LocalDate;

public record WatchHistoryDateResponse(
        LocalDate date,
        Long count) {
}
