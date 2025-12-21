package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.client.InteractionRecommenderClient;
import com.pbl6.cinemate.movie.client.dto.HealthResponse;
import com.pbl6.cinemate.movie.client.dto.RecommendationResponse;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.repository.MovieActorRepository;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieDirectorRepository;
import com.pbl6.cinemate.movie.service.InteractionService;
import com.pbl6.cinemate.movie.util.MovieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of InteractionService
 * Gets recommendations and enriches them with movie details
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionServiceImpl implements InteractionService {

    private final InteractionRecommenderClient interactionClient;
    private final MovieRepository movieRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieDirectorRepository movieDirectorRepository;

    // ============== Recommendations with Full Movie Details ==============

    @Override
    public List<MovieWithScoreResponse> getRecommendedMovies(UUID userId, int count, String context) {
        try {
            // Get raw recommendations from interaction service
            RecommendationResponse rawResponse = interactionClient.getRecommendations(userId, count, context, true);

            if (rawResponse.isEmpty()) {
                log.debug("No recommendations found for userId={}", userId);
                return List.of();
            }

            // Enrich with movie details and sort by score
            return enrichRecommendations(rawResponse);

        } catch (Exception e) {
            log.error("Failed to get recommendations: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<MovieWithScoreResponse> getHomeRecommendations(UUID userId, int count) {
        return getRecommendedMovies(userId, count, "home");
    }

    @Override
    public List<MovieWithScoreResponse> getSimilarMovies(UUID userId, UUID movieId, int count) {
        // For similar movies, we pass movie context and the service handles it
        return getRecommendedMovies(userId, count, "similar:" + movieId.toString());
    }

    @Override
    public RecommendationResponse getRawRecommendations(UUID userId, int count, String context) {
        return interactionClient.getRecommendations(userId, count, context, true);
    }

    // ============== Health ==============

    @Override
    public boolean isServiceHealthy() {
        try {
            HealthResponse health = interactionClient.getHealth();
            return health.isHealthy();
        } catch (Exception e) {
            log.warn("Interaction service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ============== Private Helper Methods ==============

    /**
     * Enrich raw recommendations with full movie details and return sorted list
     */
    private List<MovieWithScoreResponse> enrichRecommendations(RecommendationResponse rawResponse) {
        List<UUID> movieIds = rawResponse.getMovieIds();

        if (movieIds.isEmpty()) {
            return List.of();
        }

        // Fetch all movies in batch
        List<Movie> movies = movieRepository.findAllById(movieIds);

        // Create a map for quick lookup
        Map<UUID, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        // Create score map from recommendations
        Map<UUID, RecommendationResponse.RecommendationItem> recMap = rawResponse.recommendations().stream()
                .collect(Collectors.toMap(
                        RecommendationResponse.RecommendationItem::movieId,
                        Function.identity()));

        // Build enriched response maintaining order from recommendations
        List<MovieWithScoreResponse> items = movieIds.stream()
                .filter(movieMap::containsKey) // Only include movies that exist
                .map(movieId -> {
                    Movie movie = movieMap.get(movieId);
                    RecommendationResponse.RecommendationItem recItem = recMap.get(movieId);

                    // Get movie details
                    MovieResponse movieResponse = buildMovieResponse(movie);

                    return new MovieWithScoreResponse(
                            movieResponse.id(),
                            movieResponse.title(),
                            movieResponse.description(),
                            movieResponse.status(),
                            movieResponse.horizontalPoster(),
                            movieResponse.verticalPoster(),
                            movieResponse.releaseDate(),
                            movieResponse.trailerUrl(),
                            movieResponse.age(),
                            movieResponse.year(),
                            movieResponse.country(),
                            movieResponse.isVip(),
                            movieResponse.rank(),
                            movieResponse.duration(),
                            movieResponse.categories(),
                            movieResponse.actors(),
                            movieResponse.directors(),
                            recItem != null ? recItem.score() : 0.0);
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score())) // Sort by score descending
                .limit(10) // Return top 10
                .toList();

        return items;
    }

    /**
     * Build MovieResponse with categories, actors, directors
     */
    private MovieResponse buildMovieResponse(Movie movie) {
        UUID movieId = movie.getId();

        // Get categories
        List<CategoryResponse> categories = movieCategoryRepository.findByMovieIdWithCategory(movieId)
                .stream()
                .map(mc -> CategoryResponse.builder()
                        .id(mc.getCategory().getId())
                        .name(mc.getCategory().getName())
                        .build())
                .toList();

        // Get actors
        List<ActorResponse> actors = movieActorRepository.findByMovieIdWithActor(movieId)
                .stream()
                .map(ma -> ActorResponse.builder()
                        .id(ma.getActor().getId())
                        .fullname(ma.getActor().getFullname())
                        .biography(ma.getActor().getBiography())
                        .avatar(ma.getActor().getAvatar())
                        .dateOfBirth(ma.getActor().getDateOfBirth())
                        .build())
                .toList();

        // Get directors
        List<DirectorResponse> directors = movieDirectorRepository.findByMovieIdWithDirector(movieId)
                .stream()
                .map(md -> DirectorResponse.builder()
                        .id(md.getDirector().getId())
                        .fullname(md.getDirector().getFullname())
                        .biography(md.getDirector().getBiography())
                        .avatar(md.getDirector().getAvatar())
                        .dateOfBirth(md.getDirector().getDateOfBirth())
                        .build())
                .toList();

        return MovieUtils.mapToMovieResponse(movie, categories, actors, directors);
    }
}
