package com.pbl6.cinemate.movie.service.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.MovieInfoResponse;
import com.pbl6.cinemate.movie.dto.response.MovieStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.exception.InternalServerException;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.MovieService;
import com.pbl6.cinemate.movie.event.MovieCreatedEvent;

import org.springframework.context.ApplicationEventPublisher;

@Service
public class MovieServiceImpl implements MovieService {
    private final MinioStorageService minio;
    private final MovieRepository repo;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper mapper;

    public MovieServiceImpl(MinioStorageService minio, MovieRepository repo,
            ApplicationEventPublisher eventPublisher, ObjectMapper mapper) {
        this.minio = minio;
        this.repo = repo;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req) {
        Movie movie = repo.save(new Movie(req.title(), req.description(), MovieStatus.PENDING));

        Path tmp = createTempFile();
        transferFile(file, tmp);

        minio.save(tmp.toFile(), movie.getId() + "/original/" + file.getOriginalFilename());

        // Publish event to start transcoding after transaction commits
        eventPublisher.publishEvent(new MovieCreatedEvent(this, movie.getId(), tmp));

        return new MovieUploadResponse(movie.getId(), movie.getStatus().name());
    }

    @Override
    public MovieStatusResponse getMovieStatus(UUID movieId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));
        Map<String, String> qualities = parseQualitiesJson(movie.getQualitiesJson());
        return new MovieStatusResponse(movie.getId(), movie.getStatus().name(), qualities);
    }

    @Override
    public MovieInfoResponse getMovieInfo(UUID movieId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));
        Map<String, String> qualities = parseQualitiesJson(movie.getQualitiesJson());
        return new MovieInfoResponse(movie.getId(), movie.getTitle(), movie.getDescription(),
                movie.getStatus().name(), qualities);
    }

    private Path createTempFile() {
        try {
            return Files.createTempFile("movie-", ".mp4");
        } catch (Exception e) {
            throw new InternalServerException("Failed to create temporary file: " + e.getMessage());
        }
    }

    private void transferFile(MultipartFile file, Path destination) {
        try {
            file.transferTo(destination);
        } catch (Exception e) {
            throw new InternalServerException("Failed to transfer uploaded file: " + e.getMessage());
        }
    }

    private Map<String, String> parseQualitiesJson(String qualitiesJson) {
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
}
