package com.pbl6.cinemate.movie.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.pbl6.cinemate.movie.service.MovieTranscodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener for movie-related events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MovieEventListener {

    private final MovieTranscodeService movieTranscodeService;

    /**
     * Handle movie creation event after the transaction is committed
     * This ensures the movie record is persisted before transcoding starts
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMovieCreated(MovieCreatedEvent event) {
        log.info("Handling movie created event for movie: {}", event.getMovieId());

        try {
            movieTranscodeService.transcodeMovie(event.getMovieId(), event.getInputFile());
        } catch (Exception e) {
            log.error("Failed to start transcoding for movie: {}", event.getMovieId(), e);
        }
    }
}