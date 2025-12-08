package com.pbl6.microservices.customer_service.payload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pbl6.microservices.customer_service.client.dto.MovieDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FavoriteResponse {
    private UUID id;
    private UUID movieId;
    private LocalDateTime createdAt;
    
    // Enriched movie details
    private MovieDetailResponse movie;
}

