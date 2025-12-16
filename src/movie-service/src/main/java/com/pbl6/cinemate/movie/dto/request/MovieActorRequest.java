package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieActorRequest {
    @NotEmpty(message = "Actor IDs list cannot be empty")
    private List<UUID> actorIds;
}
