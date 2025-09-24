package com.pbl6.cinemate.auth_service.payload.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    UUID id;
    String name;
    String description;
}
