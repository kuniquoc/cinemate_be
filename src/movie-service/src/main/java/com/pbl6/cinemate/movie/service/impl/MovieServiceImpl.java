package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.event.MovieCreatedEvent;
import com.pbl6.cinemate.movie.exception.InternalServerException;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.MovieService;
import com.pbl6.cinemate.movie.util.MovieUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MovieServiceImpl implements MovieService {
    private final MinioStorageService minio;
    private final MovieRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${minio.movie-bucket:}")
    private String movieBucket;

    public MovieServiceImpl(MinioStorageService minio, MovieRepository repo,
            ApplicationEventPublisher eventPublisher) {
        this.minio = minio;
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req) {
        Movie movie = repo.save(new Movie(req.title(), req.description(), MovieStatus.PENDING));

        Path tmp = createTempFile();
        if (tmp == null) {
            throw new InternalServerException("Failed to create temporary file for upload");
        }
        transferFile(file, tmp);

        String objectPath = String.join("/", "original", movie.getId().toString(),
                file.getOriginalFilename());
        minio.save(tmp.toFile(), movieBucket, objectPath);

        // Publish event to start transcoding after transaction commits
        eventPublisher.publishEvent(new MovieCreatedEvent(this, movie.getId(), tmp));

        return new MovieUploadResponse(movie.getId(), movie.getStatus().name());
    }

    @Override
    public MovieStatusResponse getMovieStatus(@NonNull UUID movieId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));
        Map<String, String> qualities = MovieUtils.parseQualitiesJson(movie.getQualitiesJson());
        return new MovieStatusResponse(movie.getId(), movie.getStatus().name(), qualities);
    }

    @Override
    public MovieInfoResponse getMovieInfo(@NonNull UUID movieId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));
        return MovieUtils.mapToMovieInfoResponse(movie);
    }

    @Override
    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = repo.findAll();
        return movies.stream().map(MovieUtils::mapToMovieResponse).toList();
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest movieRequest) {
        Movie movie = MovieUtils.mapToMovie(movieRequest);
        if (movie == null) {
            throw new InternalServerException("Failed to map MovieRequest to Movie entity");
        }
        Movie savedMovie = repo.save(movie);
        return MovieUtils.mapToMovieResponse(savedMovie);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(@NonNull UUID movieId, MovieRequest movieRequest) {
        Movie movie = repo.findById(movieId).orElseThrow(() -> new NotFoundException("Movie not found"));
        movie.setTitle(movieRequest.getTitle());
        movie.setDescription(movieRequest.getDescription());
        Movie updatedMovie = repo.save(movie);
        return MovieUtils.mapToMovieResponse(updatedMovie);
    }

    @Override
    @Transactional
    public void deleteMovie(@NonNull UUID movieId) {
        if (!repo.existsById(movieId)) {
            throw new NotFoundException("Movie not found");
        }
        repo.deleteById(movieId);
    }

    @Override
    public PaginatedResponse<MovieResponse> getMovies(int page, int size, String sortBy,
            @NonNull String sortDirection) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        var moviePage = repo.findAll(pageable);
        List<MovieResponse> movies = moviePage.getContent().stream().map(MovieUtils::mapToMovieResponse).toList();
        return new PaginatedResponse<>(movies, moviePage.getNumber(), moviePage.getSize(), moviePage.getTotalPages());
    }

    private Path createTempFile() {
        try {
            return Files.createTempFile("movie-", ".mp4");
        } catch (Exception e) {
            throw new InternalServerException("Failed to create temporary file: " + e.getMessage());
        }
    }

    private void transferFile(MultipartFile file, @NonNull Path destination) {
        try {
            file.transferTo(destination);
        } catch (Exception e) {
            throw new InternalServerException("Failed to transfer uploaded file: " + e.getMessage());
        }
    }

}
