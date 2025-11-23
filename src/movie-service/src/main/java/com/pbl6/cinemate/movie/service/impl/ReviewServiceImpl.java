package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.ReviewCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ReviewUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ReviewResponse;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.Review;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.repository.ReviewRepository;
import com.pbl6.cinemate.movie.service.ReviewService;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    @Transactional
    @Override
    public ReviewResponse createReview(UUID movieId, UUID customerId, ReviewCreationRequest request) {
        log.info("Creating review for movie ID: {} by customer ID: {}", movieId, customerId);

        // Validate movie exists
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        // Check if customer already reviewed this movie
        Optional<Review> existingReview = reviewRepository.findByMovieIdAndCustomerIdAndDeletedAtIsNull(movieId,
                customerId);
        if (existingReview.isPresent()) {
            throw new BadRequestException("Customer has already reviewed this movie");
        }

        // TODO: validate customer id exists in customer service

        Review review = new Review(movie, customerId, request.content(), request.stars(),
                request.userName(), request.userAvatar());
        Review savedReview = reviewRepository.save(review);

        return mapToReviewResponse(savedReview);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getAllReviews() {
        log.info("Getting all reviews");

        List<Review> reviews = reviewRepository.findAllActiveReviews();

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getReviewsByMovieId(UUID movieId) {
        log.info("Getting reviews for movie ID: {}", movieId);

        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with ID: " + movieId);
        }

        List<Review> reviews = reviewRepository.findByMovieIdAndDeletedAtIsNull(movieId);

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getReviewsByCustomerId(UUID customerId) {
        log.info("Getting reviews for customer ID: {}", customerId);

        List<Review> reviews = reviewRepository.findByCustomerIdAndDeletedAtIsNull(customerId);

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ReviewResponse getReviewById(UUID reviewId) {
        log.info("Getting review by ID: {}", reviewId);

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with ID: " + reviewId));

        return mapToReviewResponse(review);
    }

    @Transactional
    @Override
    public ReviewResponse updateReview(UUID reviewId, UUID customerId, ReviewUpdateRequest request) {
        log.info("Updating review ID: {} by customer ID: {}", reviewId, customerId);

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with ID: " + reviewId));

        // Check if the review belongs to the customer
        if (!review.getCustomerId().equals(customerId)) {
            throw new BadRequestException("You can only update your own reviews");
        }

        // Update fields if provided
        if (request.content() != null) {
            review.setContent(request.content());
        }
        if (request.stars() != null) {
            review.setStars(request.stars());
        }
        Review updatedReview = reviewRepository.save(review);

        return mapToReviewResponse(updatedReview);
    }

    @Transactional
    @Override
    public void deleteReview(UUID reviewId, UUID customerId) {
        log.info("Deleting review ID: {} by customer ID: {}", reviewId, customerId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with ID: " + reviewId));

        // Check if the review belongs to the customer
        if (!review.getCustomerId().equals(customerId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    @Override
    public Double getAverageRatingByMovieId(UUID movieId) {
        log.info("Getting average rating for movie ID: {}", movieId);

        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with ID: " + movieId);
        }

        return reviewRepository.findAverageStarsByMovieId(movieId);
    }

    @Transactional(readOnly = true)
    @Override
    public Long getReviewCountByMovieId(UUID movieId) {
        log.info("Getting review count for movie ID: {}", movieId);

        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with ID: " + movieId);
        }

        return reviewRepository.countByMovieIdAndDeletedAtIsNull(movieId);
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovie().getId())
                .customerId(review.getCustomerId())
                .content(review.getContent())
                .stars(review.getStars())
                .userName(review.getUserName())
                .userAvatar(review.getUserAvatar())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
