package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.client.MovieServiceClient;
import com.pbl6.microservices.customer_service.client.dto.MovieDetailResponse;
import com.pbl6.microservices.customer_service.client.dto.MovieServiceResponse;
import com.pbl6.microservices.customer_service.entity.Favorite;
import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final MovieServiceClient movieServiceClient;

    @Override
    @Transactional
    public FavoriteResponse addFavorite(UUID customerId, FavoriteCreateRequest request) {
//        TO D0: Validate movie existence via Movie Service
        if (favoriteRepository.existsByCustomerIdAndMovieId(customerId, request.getMovieId())) {
            throw new IllegalArgumentException("Movie already in favorites");
        }
        Favorite favorite = Favorite.builder()
                .customerId(customerId)
                .movieId(request.getMovieId())
                .createdAt(LocalDateTime.now())
                .build();
        favorite = favoriteRepository.save(favorite);
        return toResponse(favorite);
    }

    @Override
    public List<FavoriteResponse> getFavorites(UUID customerId) {
        List<Favorite> favorites = favoriteRepository.findByCustomerId(customerId);
        return enrichFavorites(favorites);
    }

    @Override
    public Page<FavoriteResponse> getFavorites(UUID customerId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritePage = favoriteRepository.findByCustomerId(customerId, pageable);
        
        // Enrich with movie details
        List<FavoriteResponse> enrichedFavorites = enrichFavorites(favoritePage.getContent());
        
        return new PageImpl<>(enrichedFavorites, pageable, favoritePage.getTotalElements());
    }

    @Override
    @Transactional
    public void removeFavorite(UUID customerId, UUID movieId) {
//        TO DO: Validate movie existence via Movie Service
        favoriteRepository.deleteByCustomerIdAndMovieId(customerId, movieId);
    }

    // Enrich favorites with movie details
    private List<FavoriteResponse> enrichFavorites(List<Favorite> favorites) {
        return favorites.stream()
                .map(favorite -> {
                    try {
                        MovieServiceResponse response = movieServiceClient.getMovieById(favorite.getMovieId());
                        if (response != null && response.getData() != null) {
                            return toResponseWithMovie(favorite, response.getData());
                        }
                        log.warn("Movie service returned null data for movieId={}", favorite.getMovieId());
                        return toResponse(favorite);
                    } catch (Exception e) {
                        log.warn("Failed to fetch movie details for movieId={}: {}", 
                                favorite.getMovieId(), e.getMessage());
                        // Fallback: return without movie details
                        return toResponse(favorite);
                    }
                })
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

