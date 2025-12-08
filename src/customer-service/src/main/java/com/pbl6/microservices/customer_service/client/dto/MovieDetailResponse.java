package com.pbl6.microservices.customer_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO matching movie-service's MovieInfoResponse
 * Matches the structure returned by GET /api/movies/{id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetailResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private List<String> qualities;
    private String verticalPoster;
    private String horizontalPoster;
    private LocalDate releaseDate;
    private String trailerUrl;
    private Integer age;
    private Integer year;
    private String country;
    private Boolean isVip;
    private Integer rank;
    private List<Object> actors;        // Can be detailed later if needed
    private List<Object> directors;     // Can be detailed later if needed
    private List<Object> categories;    // Can be detailed later if needed
    private Long lastWatchedPosition;
}
