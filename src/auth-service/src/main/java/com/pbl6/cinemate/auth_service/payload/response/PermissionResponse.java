package com.pbl6.cinemate.auth_service.payload.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.persistence.criteria.CriteriaBuilder.In;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponse {
    UUID id;
    String name;
    String description;
    Instant createdAt;
    Instant updatedAt;
}
