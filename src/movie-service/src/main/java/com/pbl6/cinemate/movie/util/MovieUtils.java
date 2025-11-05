package com.pbl6.cinemate.movie.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.response.MovieInfoResponse;
import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.exception.InternalServerException;

import java.util.Map;

public final class MovieUtils {
    public static MovieResponse mapToMovieResponse(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(),
                movie.getHorizontalPoster(), movie.getVerticalPoster());
    }

    public static Map<String, String> parseQualitiesJson(String qualitiesJson) {
        ObjectMapper mapper = new ObjectMapper();

        if (qualitiesJson == null) {
            return Map.of();
        }
        try {
            return mapper.readValue(qualitiesJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new InternalServerException("Failed to parse movie qualities JSON: " + e.getMessage());
        }
    }

    public static MovieInfoResponse mapToMovieInfoResponse(Movie movie) {
        Map<String, String> qualities = parseQualitiesJson(movie.getQualitiesJson());
        return new MovieInfoResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getStatus().name(),
                qualities,
                movie.getVerticalPoster(),
                movie.getHorizontalPoster(),
                movie.getReleaseDate(),
                movie.getTrailerUrl(),
                movie.getAge(),
                movie.getYear(),
                movie.getCountry()
        );
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
                .build();
    }

    private MovieUtils() {
    }
}
