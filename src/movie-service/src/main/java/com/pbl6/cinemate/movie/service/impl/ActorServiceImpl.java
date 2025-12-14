package com.pbl6.cinemate.movie.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbl6.cinemate.movie.dto.request.ActorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ActorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.entity.Actor;
import com.pbl6.cinemate.movie.repository.ActorRepository;
import com.pbl6.cinemate.movie.repository.MovieActorRepository;
import com.pbl6.cinemate.movie.service.ActorService;
import com.pbl6.cinemate.shared.exception.NotFoundException;

@Service
public class ActorServiceImpl implements ActorService {

    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;

    public ActorServiceImpl(ActorRepository actorRepository, MovieActorRepository movieActorRepository) {
        this.actorRepository = actorRepository;
        this.movieActorRepository = movieActorRepository;
    }

    @Override
    public ActorResponse createActor(ActorCreationRequest request) {
        Actor actor = Actor.builder()
                .fullname(request.fullname())
                .biography(request.biography())
                .avatar(request.avatar())
                .dateOfBirth(request.dateOfBirth())
                .build();

        Actor savedActor = actorRepository.save(actor);

        return mapToActorResponse(savedActor);
    }

    @Override
    public List<ActorResponse> getAllActors() {
        return actorRepository.findAll().stream()
                .filter(actor -> actor.getDeletedAt() == null)
                .map(this::mapToActorResponse)
                .toList();
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
    @Transactional
    public void deleteActor(UUID id) {
        if (!actorRepository.existsById(id)) {
            throw new NotFoundException("Actor not found with id: " + id);
        }
        // Delete all movie-actor relationships first
        movieActorRepository.deleteByActorId(id);
        movieActorRepository.flush();
        // Then delete the actor
        actorRepository.deleteById(id);
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
