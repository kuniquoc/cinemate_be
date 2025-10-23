package com.pbl6.microservices.customer_service.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FavoriteCreateRequest {
    @NotNull
    private UUID movieId;
}

