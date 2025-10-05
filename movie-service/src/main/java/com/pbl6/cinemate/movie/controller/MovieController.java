package com.pbl6.cinemate.movie.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.MovieInfoResponse;
import com.pbl6.cinemate.movie.dto.response.MovieStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;
import com.pbl6.cinemate.movie.service.MovieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movie Management", description = "Direct movie upload and information retrieval")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @Operation(summary = "Upload movie", description = "Upload a movie file directly (recommended for files < 100MB)")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ResponseData> upload(
            @Parameter(description = "Movie file to upload") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Movie metadata") @RequestPart("data") MovieUploadRequest req,
            HttpServletRequest httpServletRequest) {

        MovieUploadResponse response = movieService.upload(file, req);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie uploaded successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movie status", description = "Get the processing status and available qualities of a movie")
    @GetMapping("/{id}/status")
    public ResponseEntity<ResponseData> status(
            @Parameter(description = "Movie ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        MovieStatusResponse response = movieService.getMovieStatus(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie status retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get movie information", description = "Get detailed information about a movie including title, description, and available qualities")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> info(
            @Parameter(description = "Movie ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        MovieInfoResponse response = movieService.getMovieInfo(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Movie information retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
