package com.pbl6.microservices.customer_service.service;

import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;

import java.util.List;
import java.util.UUID;

public interface FavoriteService {
    FavoriteResponse addFavorite(UUID customerId, FavoriteCreateRequest request);

    List<FavoriteResponse> getFavorites(UUID customerId);

    void removeFavorite(UUID customerId, UUID movieId);
}

