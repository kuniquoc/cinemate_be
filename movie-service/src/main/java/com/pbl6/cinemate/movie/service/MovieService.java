package com.pbl6.cinemate.movie.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.MovieInfoResponse;
import com.pbl6.cinemate.movie.dto.response.MovieStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;

public interface MovieService {
    MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req);

    MovieStatusResponse getMovieStatus(UUID movieId);

    MovieInfoResponse getMovieInfo(UUID movieId);
}