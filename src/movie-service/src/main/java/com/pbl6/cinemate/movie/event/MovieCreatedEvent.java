package com.pbl6.cinemate.movie.event;

import java.nio.file.Path;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a movie is successfully created and ready for
 * transcoding
 */
public class MovieCreatedEvent extends ApplicationEvent {

    private final UUID movieId;
    private final transient Path inputFile;

    public MovieCreatedEvent(Object source, UUID movieId, Path inputFile) {
        super(source);
        this.movieId = movieId;
        this.inputFile = inputFile;
    }

    public UUID getMovieId() {
        return movieId;
    }

    public Path getInputFile() {
        return inputFile;
    }
}