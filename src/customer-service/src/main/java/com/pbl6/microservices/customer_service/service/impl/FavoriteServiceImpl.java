package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.entity.Favorite;
import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;
import com.pbl6.microservices.customer_service.repository.FavoriteRepository;
import com.pbl6.microservices.customer_service.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;

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
        return favoriteRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFavorite(UUID customerId, UUID movieId) {
//        TO DO: Validate movie existence via Movie Service
        favoriteRepository.deleteByCustomerIdAndMovieId(customerId, movieId);
    }

    private FavoriteResponse toResponse(Favorite favorite) {
        FavoriteResponse response = new FavoriteResponse();
        response.setId(favorite.getId());
        response.setMovieId(favorite.getMovieId());
        response.setCreatedAt(favorite.getCreatedAt());
        return response;
    }
}

