package com.pbl6.cinemate.movie.scheduler;

import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieRankingScheduler {

    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2:00 AM
    @Transactional
    public void updateMovieRankings() {
        log.info("Starting daily movie ranking update");

        try {
            List<Movie> allMovies = movieRepository.findAll();

            // Calculate average rating for each movie and sort by rating
            List<MovieRating> movieRatings = allMovies.stream()
                    .map(movie -> {
                        Double avgRating = reviewRepository.findAverageStarsByMovieId(movie.getId());
                        return new MovieRating(movie, avgRating != null ? avgRating : 0.0);
                    })
                    .sorted((a, b) -> Double.compare(b.averageRating(), a.averageRating())) // Sort descending
                    .toList();

            // Assign ranks based on sorted order
            int currentRank = 1;
            int updatedCount = 0;

            for (MovieRating movieRating : movieRatings) {
                try {
                    Movie movie = movieRating.movie();
                    movie.setRank(currentRank);
                    movieRepository.save(movie);
                    updatedCount++;
                    currentRank++;

                    log.debug("Updated rank for movie {} ({}): {} (avg rating: {})",
                            movie.getId(), movie.getTitle(), movie.getRank(), movieRating.averageRating());
                } catch (Exception e) {
                    log.error("Failed to update rank for movie {}: {}",
                            movieRating.movie().getId(), e.getMessage());
                }
            }

            log.info("Completed movie ranking update. Updated {} movies", updatedCount);
        } catch (Exception e) {
            log.error("Failed to update movie rankings: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour for statistics
    @Transactional(readOnly = true)
    public void logRankingStatistics() {
        try {
            List<Movie> allMovies = movieRepository.findAll();

            double averageRank = allMovies.stream()
                    .filter(m -> m.getRank() != null)
                    .mapToDouble(Movie::getRank)
                    .average()
                    .orElse(0.0);

            long moviesWithReviews = allMovies.stream()
                    .filter(m -> m.getRank() != null && m.getRank() > 0)
                    .count();

            log.info("Movie ranking statistics - Total movies: {}, Movies with reviews: {}, Average rank: {:.2f}",
                    allMovies.size(), moviesWithReviews, averageRank);
        } catch (Exception e) {
            log.error("Failed to log ranking statistics: {}", e.getMessage());
        }
    }

    private record MovieRating(Movie movie, Double averageRating) {
    }
}
