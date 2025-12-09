package com.pbl6.cinemate.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for admin dashboard chart data
 * Represents viewing/favorite statistics grouped by date and category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataResponse {
    private LocalDate date;
    private long watchingView;
    private long favoriteCount;
    private String category;
}
