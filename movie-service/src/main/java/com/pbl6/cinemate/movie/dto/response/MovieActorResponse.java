package com.pbl6.cinemate.movie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieActorResponse {
    private List<ActorResponse> actors;
    private int totalActors;
}
