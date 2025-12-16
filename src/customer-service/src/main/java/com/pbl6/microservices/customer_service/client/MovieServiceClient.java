package com.pbl6.microservices.customer_service.client;

import com.pbl6.microservices.customer_service.client.dto.MovieServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for Movie Service
 */
@FeignClient(
        name = "movie-service",
        url = "${movie.service.url}"
)
public interface MovieServiceClient {

    /**
     * Get movie information by ID
     * Calls: GET /api/movies/{id}
     * Returns: ResponseData wrapper containing MovieInfoResponse
     */
    @GetMapping("/api/movies/{id}")
    MovieServiceResponse getMovieById(@PathVariable("id") UUID id);
}
