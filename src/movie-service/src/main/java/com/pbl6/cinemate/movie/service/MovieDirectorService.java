package com.pbl6.cinemate.movie.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbl6.cinemate.movie.dto.request.MovieDirectorRequest;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;
import com.pbl6.cinemate.movie.dto.response.MovieDirectorResponse;
import com.pbl6.cinemate.movie.entity.Director;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.MovieDirector;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.DirectorRepository;
import com.pbl6.cinemate.movie.repository.MovieDirectorRepository;
import com.pbl6.cinemate.movie.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieDirectorService {

    private final MovieRepository movieRepository;
    private final DirectorRepository directorRepository;
    private final MovieDirectorRepository movieDirectorRepository;

    @Transactional
    public MovieDirectorResponse addDirectorsToMovie(UUID movieId, MovieDirectorRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        List<Director> directors = directorRepository.findAllById(request.directorIds());
        if (directors.isEmpty()) {
            throw new NotFoundException("No directors found with the provided IDs");
        }

        List<MovieDirector> newMovieDirectors = directors.stream()
                .map(director -> new MovieDirector(movie, director))
                .collect(Collectors.toList());

        movieDirectorRepository.saveAll(newMovieDirectors);

        return buildMovieDirectorResponse(movieId);
    }

    public MovieDirectorResponse getDirectorsByMovieId(UUID movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with id: " + movieId);
        }
        return buildMovieDirectorResponse(movieId);
    }

    @Transactional
    public MovieDirectorResponse updateMovieDirectors(UUID movieId, MovieDirectorRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        // Delete existing directors and flush to prevent duplicates
        movieDirectorRepository.deleteByMovieId(movieId);
        movieDirectorRepository.flush();

        // Add new directors
        List<Director> directors = directorRepository.findAllById(request.directorIds());
        if (directors.isEmpty()) {
            throw new NotFoundException("No directors found with the provided IDs");
        }

        List<MovieDirector> newMovieDirectors = directors.stream()
                .map(director -> new MovieDirector(movie, director))
                .collect(Collectors.toList());

        movieDirectorRepository.saveAll(newMovieDirectors);

        return buildMovieDirectorResponse(movieId);
    }

    private MovieDirectorResponse buildMovieDirectorResponse(UUID movieId) {
        List<DirectorResponse> directors = movieDirectorRepository.findByMovieIdWithDirector(movieId)
                .stream()
                .map(movieDirector -> DirectorResponse.builder()
                        .id(movieDirector.getDirector().getId())
                        .fullname(movieDirector.getDirector().getFullname())
                        .biography(movieDirector.getDirector().getBiography())
                        .avatar(movieDirector.getDirector().getAvatar())
                        .dateOfBirth(movieDirector.getDirector().getDateOfBirth())
                        .createdAt(movieDirector.getDirector().getCreatedAt())
                        .updatedAt(movieDirector.getDirector().getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return new MovieDirectorResponse(movieId, directors);
    }
}
