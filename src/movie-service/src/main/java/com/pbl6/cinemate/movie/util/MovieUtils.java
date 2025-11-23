package com.pbl6.cinemate.movie.util;

import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.dto.response.CategoryResponse;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;
import com.pbl6.cinemate.movie.dto.response.MovieInfoResponse;
import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import com.pbl6.cinemate.movie.entity.Movie;

import java.util.List;

public final class MovieUtils {
    public static MovieResponse mapToMovieResponse(Movie movie, List<CategoryResponse> categories,
            List<ActorResponse> actors, List<DirectorResponse> directors) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getStatus() != null ? movie.getStatus().name() : null,
                movie.getHorizontalPoster(),
                movie.getVerticalPoster(),
                movie.getReleaseDate(),
                movie.getTrailerUrl(),
                movie.getAge(),
                movie.getYear(),
                movie.getCountry(),
                movie.getIsVip(),
                movie.getRank(),
                categories,
                actors,
                directors);
    }

    public static MovieInfoResponse mapToMovieInfoResponse(Movie movie, List<ActorResponse> actors,
            List<DirectorResponse> directors, List<CategoryResponse> categories) {
        List<String> qualities = movie.getQualities() != null ? movie.getQualities() : List.of();
        return new MovieInfoResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getStatus() != null ? movie.getStatus().name() : null,
                qualities,
                movie.getVerticalPoster(),
                movie.getHorizontalPoster(),
                movie.getReleaseDate(),
                movie.getTrailerUrl(),
                movie.getAge(),
                movie.getYear(),
                movie.getCountry(),
                movie.getIsVip(),
                movie.getRank(),
                actors,
                directors,
                categories);
    }

    public static Movie mapToMovie(MovieRequest movieRequest) {
        return Movie.builder()
                .age(movieRequest.getAge())
                .country(movieRequest.getCountry())
                .description(movieRequest.getDescription())
                .horizontalPoster(movieRequest.getHorizontalPoster())
                .verticalPoster(movieRequest.getVerticalPoster())
                .releaseDate(movieRequest.getReleaseDate())
                .title(movieRequest.getTitle())
                .trailerUrl(movieRequest.getTrailerUrl())
                .year(movieRequest.getYear())
                .isVip(Boolean.TRUE.equals(movieRequest.getIsVip()))
                .build();
    }

    private MovieUtils() {
    }
}
