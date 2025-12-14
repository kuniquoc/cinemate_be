package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.client.dto.RecommendationResponse;
import com.pbl6.cinemate.movie.dto.response.RecommendedMoviesResponse;
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
    @Operation(summary = "Get personalized recommendations with full movie details", description = "Returns personalized movie recommendations with complete movie information")
    public ResponseEntity<RecommendedMoviesResponse> getRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "home") String context) {
        RecommendedMoviesResponse response = interactionService.getRecommendedMovies(userId, count, context);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/home")
    @Operation(summary = "Get home page recommendations", description = "Returns recommendations for the home page with full movie details")
    public ResponseEntity<RecommendedMoviesResponse> getHomeRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int count) {
        RecommendedMoviesResponse response = interactionService.getHomeRecommendations(userId, count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/similar/{movieId}")
    @Operation(summary = "Get similar movies", description = "Returns movies similar to the specified movie")
    public ResponseEntity<RecommendedMoviesResponse> getSimilarMovies(
            @PathVariable UUID userId,
            @PathVariable UUID movieId,
            @RequestParam(defaultValue = "10") int count) {
        RecommendedMoviesResponse response = interactionService.getSimilarMovies(userId, movieId, count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/raw")
    @Operation(summary = "Get raw recommendations (IDs only)", description = "Returns raw recommendation data without enriched movie details")
    public ResponseEntity<RecommendationResponse> getRawRecommendations(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "home") String context) {
        RecommendationResponse response = interactionService.getRawRecommendations(userId, count, context);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/ids")
    @Operation(summary = "Get recommended movie IDs only")
    public ResponseEntity<List<UUID>> getRecommendedMovieIds(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(defaultValue = "home") String context) {
        List<UUID> movieIds = interactionService.getRawRecommendations(userId, count, context).getMovieIds();
        return ResponseEntity.ok(movieIds);
    }

    @GetMapping("/health")
    @Operation(summary = "Check recommendation service health")
    public ResponseEntity<Boolean> checkServiceHealth() {
        boolean healthy = interactionService.isServiceHealthy();
        return ResponseEntity.ok(healthy);
    }
}
