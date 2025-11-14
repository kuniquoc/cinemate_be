package com.pbl6.cinemate.movie.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pbl6.cinemate.movie.dto.request.ActorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ActorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.entity.Actor;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.ActorRepository;
import com.pbl6.cinemate.movie.service.ActorService;

@Service
public class ActorServiceImpl implements ActorService {

    private final ActorRepository actorRepository;

    public ActorServiceImpl(ActorRepository actorRepository) {
        this.actorRepository = actorRepository;
    }

    @Override
    public ActorResponse createActor(ActorCreationRequest request) {
        Actor actor = new Actor(
                request.fullname(),
                request.biography(),
                request.avatar(),
                request.dateOfBirth());

        Actor savedActor = actorRepository.save(actor);

        return mapToActorResponse(savedActor);
    }

    @Override
    public List<ActorResponse> getAllActors() {
        return actorRepository.findAll().stream()
                .filter(actor -> actor.getDeletedAt() == null)
                .map(this::mapToActorResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ActorResponse getActorById(UUID id) {
        Actor actor = actorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actor not found with id: " + id));

        if (actor.getDeletedAt() != null) {
            throw new NotFoundException("Actor not found with id: " + id);
        }

        return mapToActorResponse(actor);
    }

    @Override
    public ActorResponse updateActor(UUID id, ActorUpdateRequest request) {
        Actor actor = actorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actor not found with id: " + id));

        if (actor.getDeletedAt() != null) {
            throw new NotFoundException("Actor not found with id: " + id);
        }

        actor.setFullname(request.fullname());
        actor.setBiography(request.biography());
        actor.setAvatar(request.avatar());
        actor.setDateOfBirth(request.dateOfBirth());

        Actor updatedActor = actorRepository.save(actor);

        return mapToActorResponse(updatedActor);
    }

    @Override
    public void deleteActor(UUID id) {
        Actor actor = actorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actor not found with id: " + id));

        if (actor.getDeletedAt() != null) {
            throw new NotFoundException("Actor not found with id: " + id);
        }

        actorRepository.delete(actor);
    }

    private ActorResponse mapToActorResponse(Actor actor) {
        return new ActorResponse(
                actor.getId(),
                actor.getFullname(),
                actor.getBiography(),
                actor.getAvatar(),
                actor.getDateOfBirth(),
                actor.getCreatedAt(),
                actor.getUpdatedAt());
    }
}
