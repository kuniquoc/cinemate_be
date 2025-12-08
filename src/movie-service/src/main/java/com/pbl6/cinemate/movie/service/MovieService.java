package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MovieService {
    MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req);

    MovieStatusResponse getMovieStatus(@NonNull UUID movieId);

    MovieInfoResponse getMovieInfo(@NonNull UUID movieId, UUID customerId);

    List<MovieResponse> getAllMovies();

    MovieResponse createMovie(MovieRequest movieRequest);

    MovieResponse updateMovie(@NonNull UUID movieId, MovieRequest movieRequest);

    void deleteMovie(@NonNull UUID movieId);

    PaginatedResponse<MovieResponse> getMovies(String keyword, int page, int size, String sortBy,
            @NonNull String sortDirection, String userRole);

    List<MovieResponse> getTopTenMovies();

    MovieProcessStatusResponse getMovieProcessStatus(@NonNull UUID movieId);

    MovieResponse updateMovieStatus(@NonNull UUID movieId, @NonNull String newStatusString);
}