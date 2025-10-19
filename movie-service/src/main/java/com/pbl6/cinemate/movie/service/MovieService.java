package com.pbl6.cinemate.movie.service;

import java.util.List;
import java.util.UUID;

import com.pbl6.cinemate.movie.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;

public interface MovieService {
    MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req);

    MovieStatusResponse getMovieStatus(UUID movieId);
    MovieInfoResponse getMovieInfo(UUID movieId);

    List<MovieResponse> getAllMovies();
}