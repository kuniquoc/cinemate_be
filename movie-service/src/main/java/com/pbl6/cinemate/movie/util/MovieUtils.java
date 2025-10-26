package com.pbl6.cinemate.movie.util;

import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import com.pbl6.cinemate.movie.entity.Movie;

public final class MovieUtils {
    public static MovieResponse mapToMovieResponse(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(),
                movie.getHorizontalPoster(), movie.getVerticalPoster());
    }

    private MovieUtils() {
    }
}
