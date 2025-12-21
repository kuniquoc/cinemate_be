package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.client.MovieServiceClient;
import com.pbl6.microservices.customer_service.client.dto.MovieDetailResponse;
import com.pbl6.microservices.customer_service.client.dto.MovieServiceResponse;
import com.pbl6.microservices.customer_service.entity.Customer;
import com.pbl6.microservices.customer_service.entity.Favorite;
import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;
import com.pbl6.microservices.customer_service.repository.CustomerRepository;
import com.pbl6.microservices.customer_service.client.InteractionRecommenderClient;
import com.pbl6.microservices.customer_service.client.dto.FavoriteEventRequest;
import com.pbl6.microservices.customer_service.repository.FavoriteRepository;
import com.pbl6.microservices.customer_service.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MovieServiceClient movieServiceClient;
    private final CustomerRepository customerRepository;
    private final InteractionRecommenderClient interactionClient;

    @Override
    @Transactional
    public FavoriteResponse addFavorite(UUID customerId, FavoriteCreateRequest request) {
        // Ensure customer record exists (auto-create if missing)
        ensureCustomerExists(customerId);

        // TO D0: Validate movie existence via Movie Service
        if (favoriteRepository.existsByCustomerIdAndMovieId(customerId, request.getMovieId())) {
            throw new IllegalArgumentException("Movie already in favorites");
        }
        Favorite favorite = Favorite.builder()
                .customerId(customerId)
                .movieId(request.getMovieId())
                .createdAt(Instant.now())
                .build();
        favorite = favoriteRepository.save(favorite);
        // Best-effort: send favorite 'add' event
        try {
            interactionClient.trackFavoriteEvent(FavoriteEventRequest.create(customerId, request.getMovieId(), "add"));
        } catch (Exception e) {
            log.debug("Failed to send favorite add event: {}", e.getMessage());
        }
        return toResponse(favorite);
    }

    @Override
    public List<MovieDetailResponse> getFavorites(UUID customerId) {
        List<Favorite> favorites = favoriteRepository.findByCustomerId(customerId);
        return enrichFavorites(favorites);
    }

    @Override
    public Page<MovieDetailResponse> getFavorites(UUID customerId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritePage = favoriteRepository.findByCustomerId(customerId, pageable);

        log.info("favorites: {}", favoritePage.getContent());
        log.info("number {}", favoritePage.getNumber());

        // Enrich with movie details
        List<MovieDetailResponse> enrichedMovies = enrichFavorites(favoritePage.getContent());

        return new PageImpl<>(enrichedMovies, pageable, favoritePage.getTotalElements());
    }

    @Override
    @Transactional
    public void removeFavorite(UUID customerId, UUID movieId) {
        // TO DO: Validate movie existence via Movie Service
        favoriteRepository.deleteByCustomerIdAndMovieId(customerId, movieId);
        // Best-effort: send favorite 'remove' event
        try {
            interactionClient.trackFavoriteEvent(FavoriteEventRequest.create(customerId, movieId, "remove"));
        } catch (Exception e) {
            log.debug("Failed to send favorite remove event: {}", e.getMessage());
        }
    }

    /**
     * Ensures customer record exists for the given account ID.
     * Auto-creates customer if not found to satisfy FK constraint.
     */
    private void ensureCustomerExists(UUID accountId) {
        if (!customerRepository.existsByAccountId(accountId)) {
            log.info("Customer record not found for account_id: {}. Creating default customer record.", accountId);
            Customer customer = Customer.builder()
                    .accountId(accountId)
                    .firstName("User")
                    .lastName("")
                    .build();
            customerRepository.save(customer);
        }
    }

    // Enrich favorites with movie details - returns movie data directly
    private List<MovieDetailResponse> enrichFavorites(List<Favorite> favorites) {
        log.info("vao ham ");
        return favorites.stream()
                .map(favorite -> {
                    try {
                        MovieServiceResponse response = movieServiceClient.getMovieById(favorite.getMovieId());
                        if (response != null && response.getData() != null) {
                            log.info("data {}", response.getData());
                            return response.getData();
                        }
                        log.warn("Movie service returned null data for movieId={}", favorite.getMovieId());
                        return null;
                    } catch (Exception e) {
                        log.warn("Failed to fetch movie details for movieId={}: {}",
                                favorite.getMovieId(), e.getMessage());
                        return null;
                    }
                })
                .filter(movie -> movie != null) // Filter out null movies
                .collect(Collectors.toList());
    }

    private FavoriteResponse toResponse(Favorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .movieId(favorite.getMovieId())
                .createdAt(favorite.getCreatedAt())
                .build();
    }

    private FavoriteResponse toResponseWithMovie(Favorite favorite, MovieDetailResponse movie) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .movieId(favorite.getMovieId())
                .createdAt(favorite.getCreatedAt())
                .movie(movie)
                .build();
    }
}
