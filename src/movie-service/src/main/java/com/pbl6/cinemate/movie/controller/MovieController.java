package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.client.CustomerServiceClient;
import com.pbl6.cinemate.movie.dto.request.*;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.service.MovieActorService;
import com.pbl6.cinemate.movie.service.MovieDirectorService;
import com.pbl6.cinemate.movie.service.MovieService;
import com.pbl6.cinemate.movie.service.ReviewService;
import com.pbl6.cinemate.movie.service.impl.ReviewServiceImpl;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movie Management", description = "Movie management including upload, information retrieval, actors, directors and reviews")
@Slf4j
public class MovieController {
        private final MovieService movieService;
        private final MovieActorService movieActorService;
        private final MovieDirectorService movieDirectorService;
        private final ReviewService reviewService;
        private final CustomerServiceClient customerServiceClient;

        public MovieController(MovieService movieService, MovieActorService movieActorService,
                        MovieDirectorService movieDirectorService, ReviewServiceImpl reviewService,
                        CustomerServiceClient customerServiceClient) {
                this.movieService = movieService;
                this.movieActorService = movieActorService;
                this.movieDirectorService = movieDirectorService;
                this.reviewService = reviewService;
                this.customerServiceClient = customerServiceClient;
        }

        @Operation(summary = "Get movie status", description = "Get the processing status and available qualities of a movie")
        @GetMapping("/{id}/status")
        public ResponseEntity<ResponseData> status(
                        @Parameter(description = "Movie ID") @NonNull @PathVariable UUID id,
                        HttpServletRequest httpServletRequest) {

                MovieStatusResponse response = movieService.getMovieStatus(id);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Movie status retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get movie process status", description = "Get only the processing status of a movie (UPLOADING, PROCESSING, COMPLETED, FAILED)")
        @GetMapping("/{id}/process-status")
        public ResponseEntity<ResponseData> getProcessStatus(
                        @Parameter(description = "Movie ID") @NonNull @PathVariable UUID id,
                        HttpServletRequest httpServletRequest) {

                MovieProcessStatusResponse response = movieService.getMovieProcessStatus(id);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Movie process status retrieved successfully",
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

        @Operation(summary = "Get movie detail", description = "Get detailed information about a movie including title, description, and available qualities. If user is authenticated, includes last watched position.")
        @GetMapping("/{id}")
        public ResponseEntity<ResponseData> info(
                        @Parameter(description = "Movie ID") @NonNull @PathVariable UUID id,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {

                UUID customerId = userPrincipal != null ? userPrincipal.getId() : null;
                MovieInfoResponse response = movieService.getMovieInfo(id, customerId);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Movie information retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        // Movie-Actor nested endpoints
        @Operation(summary = "Add actors to movie", description = "Add a list of actors to a specific movie (Admin only)")
        @PostMapping("/{movieId}/actors")
        @PreAuthorize("hasRole('ADMIN')")
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

        @Operation(summary = "Update movie actors", description = "Replace all actors for a specific movie with the provided list (Admin only)")
        @PutMapping("/{movieId}/actors")
        @PreAuthorize("hasRole('ADMIN')")
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

        // Movie-Director nested endpoints
        @Operation(summary = "Add directors to movie", description = "Add a list of directors to a specific movie (Admin only)")
        @PostMapping("/{movieId}/directors")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ResponseData> addDirectorsToMovie(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @Valid @RequestBody MovieDirectorRequest request,
                        HttpServletRequest httpServletRequest) {

                MovieDirectorResponse response = movieDirectorService.addDirectorsToMovie(movieId, request);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Directors added to movie successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get directors by movie", description = "Get all directors associated with a specific movie")
        @GetMapping("/{movieId}/directors")
        public ResponseEntity<ResponseData> getDirectorsByMovie(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        HttpServletRequest httpServletRequest) {

                MovieDirectorResponse response = movieDirectorService.getDirectorsByMovieId(movieId);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Movie directors retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Update movie directors", description = "Replace all directors for a specific movie with the provided list (Admin only)")
        @PutMapping("/{movieId}/directors")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ResponseData> updateMovieDirectors(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @Valid @RequestBody MovieDirectorRequest request,
                        HttpServletRequest httpServletRequest) {

                MovieDirectorResponse response = movieDirectorService.updateMovieDirectors(movieId, request);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Movie directors updated successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        // Movie-Review nested endpoints
        @Operation(summary = "Create review for movie", description = "Create a new review for a specific movie by authenticated user")
        @PostMapping("/{movieId}/reviews")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ResponseData> createReviewForMovie(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @CurrentUser UserPrincipal userPrincipal,
                        @Valid @RequestBody ReviewCreationRequest request,
                        HttpServletRequest httpServletRequest) {

                // Get user display name from JWT claims
                String userName = userPrincipal.getFullName();
                if (userName == null || userName.isBlank()) {
                        userName = userPrincipal.getUsername(); // fallback to email
                }

                // Get avatar from customer-service (with circuit breaker fallback)
                String userAvatar = null;
                try {
                        var customerInfo = customerServiceClient.getCustomerInfo(userPrincipal.getId());
                        if (customerInfo != null) {
                                userAvatar = customerInfo.getAvatarUrl();
                                // Use customer name if JWT name is null
                                if ((userName == null || userName.isBlank()) && customerInfo.getFullName() != null) {
                                        userName = customerInfo.getFullName();
                                }
                        }
                } catch (Exception e) {
                        log.warn("Failed to get customer info for user {}: {}", userPrincipal.getId(), e.getMessage());
                }

                ReviewResponse response = reviewService.createReview(movieId, userPrincipal.getId(), request, userName,
                                userAvatar);

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

        @Operation(summary = "Update review for movie", description = "Update an existing review for a movie (only by the review owner)")
        @PutMapping("/{movieId}/reviews/{reviewId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ResponseData> updateReviewForMovie(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @Parameter(description = "Review ID") @PathVariable UUID reviewId,
                        @CurrentUser UserPrincipal userPrincipal,
                        @Valid @RequestBody ReviewUpdateRequest request,
                        HttpServletRequest httpServletRequest) {

                ReviewResponse response = reviewService.updateReview(reviewId, userPrincipal.getId(), request);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Review updated successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Delete review for movie", description = "Delete a review for a movie (only by the review owner)")
        @DeleteMapping("/{movieId}/reviews/{reviewId}")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ResponseData> deleteReviewForMovie(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @Parameter(description = "Review ID") @PathVariable UUID reviewId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {

                reviewService.deleteReview(reviewId, userPrincipal.getId());

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

        @Operation(summary = "Create a new movie", description = "Create a new movie with the provided details (Admin only)")
        @PostMapping
        @PreAuthorize("hasRole('ADMIN')")
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

        @Operation(summary = "Update an existing movie", description = "Update movie details by ID (Admin only)")
        @PutMapping("/{movieId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ResponseData> updateMovie(
                        @NonNull @PathVariable UUID movieId,
                        @Valid @RequestBody MovieRequest movieRequest,
                        HttpServletRequest httpServletRequest) {

                MovieResponse movieResponse = movieService.updateMovie(movieId, movieRequest);

                return ResponseEntity.ok(ResponseData.success(
                                movieResponse,
                                "Movie updated successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Update movie status", description = "Update movie publication status (PRIVATE or PUBLIC only). Cannot change to DRAFT - upload a new movie instead. (Admin only)")
        @PatchMapping("/{movieId}/status")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ResponseData> updateMovieStatus(
                        @NonNull @PathVariable UUID movieId,
                        @Valid @RequestBody UpdateMovieStatusRequest request,
                        HttpServletRequest httpServletRequest) {

                MovieResponse movieResponse = movieService.updateMovieStatus(
                                movieId,
                                Objects.requireNonNull(request.status()));

                return ResponseEntity.ok(ResponseData.success(
                                movieResponse,
                                "Movie status updated successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Delete a movie", description = "Delete a movie by ID (Admin only)")
        @DeleteMapping("/{movieId}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ResponseData> deleteMovie(
                        @NonNull @PathVariable UUID movieId,
                        HttpServletRequest httpServletRequest) {

                movieService.deleteMovie(movieId);

                return ResponseEntity.ok(ResponseData.success(
                                "Movie deleted successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get or search movies with pagination and sorting", description = "Retrieve a paginated and sorted list of movies. Optionally provide a keyword to search across title, description, country, actors, and categories. Admin users can see all movies including DRAFT and PRIVATE status.")
        @GetMapping
        public ResponseEntity<ResponseData> getMovies(
                        @Parameter(description = "Optional search keyword") @RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        @RequestParam(name = "size", defaultValue = "10") int size,
                        @RequestParam(name = "sortBy", defaultValue = "title") String sortBy,
                        @RequestParam(name = "sortDirection", defaultValue = "asc") @NonNull String sortDirection,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {

                // Determine user role from JWT
                // admin can see all movies, regular users only see PUBLIC
                String userRole = (userPrincipal != null && userPrincipal.getRole() != null
                                && userPrincipal.getRole().equalsIgnoreCase("ROLE_ADMIN")) ? "ADMIN" : "USER";

                PaginatedResponse<MovieResponse> data = movieService.getMovies(keyword, page - 1, size, sortBy,
                                sortDirection, userRole);

                String message = (keyword != null && !keyword.isBlank())
                                ? "Movies search results retrieved successfully"
                                : "Movies retrieved successfully";

                return ResponseEntity.ok(ResponseData.successWithMeta(
                                data,
                                message,
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get top ten movies", description = "Retrieve the top 10 movies ranked by their average review stars")
        @GetMapping("/top-ten")
        public ResponseEntity<ResponseData> getTopTenMovies(HttpServletRequest httpServletRequest) {
                List<MovieResponse> response = movieService.getTopTenMovies();
                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Top ten movies retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }
}
