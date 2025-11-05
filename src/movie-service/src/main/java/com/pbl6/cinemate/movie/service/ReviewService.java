package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.request.ReviewCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ReviewUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ReviewResponse;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID movieId, UUID customerId, ReviewCreationRequest request);

    List<ReviewResponse> getAllReviews();

    List<ReviewResponse> getReviewsByMovieId(UUID movieId);

    List<ReviewResponse> getReviewsByCustomerId(UUID customerId);

    ReviewResponse getReviewById(UUID reviewId);

    ReviewResponse updateReview(UUID reviewId, UUID customerId, ReviewUpdateRequest request);

    void deleteReview(UUID reviewId, UUID customerId);

    Double getAverageRatingByMovieId(UUID movieId);

    Long getReviewCountByMovieId(UUID movieId);
}
