package com.pbl6.microservices.customer_service.payload.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FavoriteResponse {
    private UUID id;
    private UUID movieId;
    private LocalDateTime createdAt;
}

