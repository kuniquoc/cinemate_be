package com.pbl6.cinemate.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID movieId;
    private UUID customerId;
    private String content;
    private Integer stars;
    private String userName;
    private String userAvatar;
    private Instant createdAt;
    private Instant updatedAt;
}
