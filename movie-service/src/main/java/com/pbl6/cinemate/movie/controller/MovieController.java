package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.dto.request.*;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.service.MovieActorService;
import com.pbl6.cinemate.movie.service.MovieService;
import com.pbl6.cinemate.movie.service.ReviewService;
import com.pbl6.cinemate.movie.service.impl.ReviewServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movie Management", description = "Movie management including upload, information retrieval, actors and reviews")
public class MovieController {
    private final MovieService movieService;
    private final MovieActorService movieActorService;
    private final ReviewService reviewService;

    public MovieController(MovieService movieService, MovieActorService movieActorService, ReviewServiceImpl reviewService) {
        this.movieService = movieService;
        this.movieActorService = movieActorService;
        this.reviewService = reviewService;
    }

    @Operation(summary = "Upload movie", description = "Upload a movie file directly (recommended for files < 100MB)")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ResponseData> upload(
            @Parameter(description = "Movie file to upload") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Movie metadata") @RequestPart("data") MovieUploadRequest req,
            HttpServletRequest httpServletRequest) {

        MovieUploadResponse response = movieService.upload(file, req);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie uploaded successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movie status", description = "Get the processing status and available qualities of a movie")
    @GetMapping("/{id}/status")
    public ResponseEntity<ResponseData> status(
            @Parameter(description = "Movie ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        MovieStatusResponse response = movieService.getMovieStatus(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie status retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    // New: Get all movies (list)
    @Operation(summary = "Get all movies", description = "Retrieve a list of movies with basic info")
    @GetMapping("/all")
    public ResponseEntity<ResponseData> getAllMovies(HttpServletRequest httpServletRequest) {
        List<MovieResponse> response = movieService.getAllMovies();
        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movies retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }


    @Operation(summary = "Get movie detail", description = "Get detailed information about a movie including title, description, and available qualities")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> info(
            @Parameter(description = "Movie ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        MovieInfoResponse response = movieService.getMovieInfo(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie information retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    // Movie-Actor nested endpoints
//    TODO: secure these endpoints to admin only
    @Operation(summary = "Add actors to movie", description = "Add a list of actors to a specific movie")
    @PostMapping("/{movieId}/actors")
    public ResponseEntity<ResponseData> addActorsToMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            @Valid @RequestBody MovieActorRequest request,
            HttpServletRequest httpServletRequest) {

        MovieActorResponse response = movieActorService.addActorsToMovie(movieId, request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Actors added to movie successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get actors by movie", description = "Get all actors associated with a specific movie")
    @GetMapping("/{movieId}/actors")
    public ResponseEntity<ResponseData> getActorsByMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            HttpServletRequest httpServletRequest) {

        MovieActorResponse response = movieActorService.getActorsByMovieId(movieId);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie actors retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    //    TODO: admin only
    @Operation(summary = "Update movie actors", description = "Replace all actors for a specific movie with the provided list")
    @PutMapping("/{movieId}/actors")
    public ResponseEntity<ResponseData> updateMovieActors(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            @Valid @RequestBody MovieActorRequest request,
            HttpServletRequest httpServletRequest) {

        MovieActorResponse response = movieActorService.updateMovieActors(movieId, request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie actors updated successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    // Movie-Review nested endpoints
//    TODO: get customer id from JWT token instead of payload
    @Operation(summary = "Create review for movie", description = "Create a new review for a specific movie by a customer")
    @PostMapping("/{movieId}/reviews")
    public ResponseEntity<ResponseData> createReviewForMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            @Valid @RequestBody ReviewCreationRequest request,
            HttpServletRequest httpServletRequest) {

        ReviewResponse response = reviewService.createReview(movieId, request.userId(), request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Review created successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get reviews for movie", description = "Retrieve all reviews for a specific movie")
    @GetMapping("/{movieId}/reviews")
    public ResponseEntity<ResponseData> getReviewsForMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            HttpServletRequest httpServletRequest) {

        List<ReviewResponse> response = reviewService.getReviewsByMovieId(movieId);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie reviews retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    //    TODO: only review owner can update
//    TODO: get customer id from JWT token instead of payload
    @Operation(summary = "Update review for movie", description = "Update an existing review for a movie (only by the review owner)")
    @PutMapping("/{movieId}/reviews/{reviewId}")
    public ResponseEntity<ResponseData> updateReviewForMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            @Parameter(description = "Review ID") @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            HttpServletRequest httpServletRequest) {

        ReviewResponse response = reviewService.updateReview(reviewId, request.userId(), request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Review updated successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    //    TODO: get customer id from JWT token instead of request param
    @Operation(summary = "Delete review for movie", description = "Delete a review for a movie (only by the review owner)")
    @DeleteMapping("/{movieId}/reviews/{reviewId}")
    public ResponseEntity<ResponseData> deleteReviewForMovie(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            @Parameter(description = "Review ID") @PathVariable UUID reviewId,
            @RequestParam UUID userId,
            HttpServletRequest httpServletRequest) {

        reviewService.deleteReview(reviewId, userId);

        return ResponseEntity.ok(ResponseData.success(
                null,
                "Review deleted successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movie average rating", description = "Get the average star rating for a specific movie")
    @GetMapping("/{movieId}/reviews/average-rating")
    public ResponseEntity<ResponseData> getMovieAverageRating(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            HttpServletRequest httpServletRequest) {

        Double averageRating = reviewService.getAverageRatingByMovieId(movieId);

        return ResponseEntity.ok(ResponseData.success(
                averageRating,
                "Average rating retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movie review count", description = "Get the total number of reviews for a specific movie")
    @GetMapping("/{movieId}/reviews/count")
    public ResponseEntity<ResponseData> getMovieReviewCount(
            @Parameter(description = "Movie ID") @PathVariable UUID movieId,
            HttpServletRequest httpServletRequest) {

        Long reviewCount = reviewService.getReviewCountByMovieId(movieId);

        return ResponseEntity.ok(ResponseData.success(
                reviewCount,
                "Review count retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Create a new movie", description = "Create a new movie with the provided details")
    @PostMapping
    public ResponseEntity<ResponseData> createMovie(
            @Valid @RequestBody MovieRequest movieRequest,
            HttpServletRequest httpServletRequest) {

        MovieResponse movieResponse = movieService.createMovie(movieRequest);

        return ResponseEntity.ok(ResponseData.success(
                movieResponse,
                "Movie created successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Update an existing movie", description = "Update movie details by ID")
    @PutMapping("/{movieId}")
    public ResponseEntity<ResponseData> updateMovie(
            @PathVariable UUID movieId,
            @Valid @RequestBody MovieRequest movieRequest,
            HttpServletRequest httpServletRequest) {

        MovieResponse movieResponse = movieService.updateMovie(movieId, movieRequest);

        return ResponseEntity.ok(ResponseData.success(
                movieResponse,
                "Movie updated successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Delete a movie", description = "Delete a movie by ID")
    @DeleteMapping("/{movieId}")
    public ResponseEntity<ResponseData> deleteMovie(
            @PathVariable UUID movieId,
            HttpServletRequest httpServletRequest) {

        movieService.deleteMovie(movieId);

        return ResponseEntity.ok(ResponseData.success(
                "Movie deleted successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movies with pagination and sorting", description = "Retrieve a paginated and sorted list of movies")
    @GetMapping
    public ResponseEntity<ResponseData> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            HttpServletRequest httpServletRequest) {

        PaginatedResponse<MovieResponse> data = movieService.getMovies(page - 1, size, sortBy, sortDirection);

        return ResponseEntity.ok(ResponseData.successWithMeta(
                data,
                "Movies retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
