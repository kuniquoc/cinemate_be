package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.request.MovieActorRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.dto.response.MovieActorResponse;
import com.pbl6.cinemate.movie.entity.Actor;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.MovieActor;
import com.pbl6.cinemate.movie.exception.BadRequestException;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.ActorRepository;
import com.pbl6.cinemate.movie.repository.MovieActorRepository;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieActorService {

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;

    @Transactional
    public MovieActorResponse addActorsToMovie(UUID movieId, MovieActorRequest request) {
        log.info("Adding actors to movie with ID: {}", movieId);

        // Validate movie exists
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        // Validate all actors exist
        List<Actor> actors = actorRepository.findAllById(request.getActorIds());
        if (actors.size() != request.getActorIds().size()) {
            throw new BadRequestException("One or more actors not found");
        }

        // Check for existing relationships and add only new ones
        List<MovieActor> newMovieActors = actors.stream()
            .filter(actor -> !movieActorRepository.existsByMovieIdAndActorId(movieId, actor.getId()))
            .map(actor -> new MovieActor(movie, actor))
            .collect(Collectors.toList());

        if (newMovieActors.isEmpty()) {
            throw new BadRequestException("All actors are already assigned to this movie");
        }

        movieActorRepository.saveAll(newMovieActors);

        // Return all actors for the movie
        return getActorsByMovieId(movieId);
    }

    @Transactional(readOnly = true)
    public MovieActorResponse getActorsByMovieId(UUID movieId) {
        log.info("Getting actors for movie with ID: {}", movieId);

        // Validate movie exists
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with ID: " + movieId);
        }

        List<MovieActor> movieActors = movieActorRepository.findByMovieIdWithActor(movieId);

        List<ActorResponse> actorResponses = movieActors.stream()
            .map(movieActor -> mapToActorResponse(movieActor.getActor()))
            .collect(Collectors.toList());

        return new MovieActorResponse(actorResponses, actorResponses.size());
    }

    @Transactional
    public MovieActorResponse updateMovieActors(UUID movieId, MovieActorRequest request) {
        log.info("Updating actors for movie with ID: {}", movieId);

        // Validate movie exists
        Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        // Validate all actors exist
        List<Actor> actors = actorRepository.findAllById(request.getActorIds());
        if (actors.size() != request.getActorIds().size()) {
            throw new BadRequestException("One or more actors not found");
        }

        // Remove all existing relationships
        movieActorRepository.deleteByMovieId(movieId);

        // Add new relationships
        List<MovieActor> newMovieActors = actors.stream()
            .map(actor -> new MovieActor(movie, actor))
            .collect(Collectors.toList());

        movieActorRepository.saveAll(newMovieActors);

        // Return updated actors list
        return getActorsByMovieId(movieId);
    }

    private ActorResponse mapToActorResponse(Actor actor) {
        return ActorResponse.builder()
            .id(actor.getId())
            .fullname(actor.getFullname())
            .biography(actor.getBiography())
            .avatar(actor.getAvatar())
            .dateOfBirth(actor.getDateOfBirth())
            .createdAt(actor.getCreatedAt())
            .updatedAt(actor.getUpdatedAt())
            .build();
    }
}
