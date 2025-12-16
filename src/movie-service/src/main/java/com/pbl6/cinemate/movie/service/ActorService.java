package com.pbl6.cinemate.movie.service;

import java.util.List;
import java.util.UUID;

import com.pbl6.cinemate.movie.dto.request.ActorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ActorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;

public interface ActorService {
    ActorResponse createActor(ActorCreationRequest request);

    List<ActorResponse> getAllActors();

    ActorResponse getActorById(UUID id);

    ActorResponse updateActor(UUID id, ActorUpdateRequest request);

    void deleteActor(UUID id);
}
