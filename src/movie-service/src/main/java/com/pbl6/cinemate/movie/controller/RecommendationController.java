package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.response.MovieWithScoreResponse;
import com.pbl6.cinemate.movie.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for movie recommendations
 * Integrates with interaction-recommender-service via InteractionService
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Movie recommendation endpoints")
public class RecommendationController {

    private final InteractionService interactionService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get personalized recommendations with full movie details", description = "Returns personalized movie recommendations with complete movie information, sorted by score descending, top 10")
    public ResponseEntity<List<MovieWithScoreResponse>> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "home") String context) {
        List<MovieWithScoreResponse> response = interactionService.getRecommendedMovies(userId, count, context);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/similar/{movieId}")
    @Operation(summary = "Get similar movies", description = "Returns movies similar to the specified movie, sorted by score descending, top 10")
    public ResponseEntity<List<MovieWithScoreResponse>> getSimilarMovies(
            @PathVariable UUID userId,
            @PathVariable UUID movieId,
            @RequestParam(defaultValue = "10") int count) {
        List<MovieWithScoreResponse> response = interactionService.getSimilarMovies(userId, movieId, count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Check recommendation service health")
    public ResponseEntity<Boolean> checkServiceHealth() {
        boolean healthy = interactionService.isServiceHealthy();
        return ResponseEntity.ok(healthy);
    }
}
