package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.entity.Category;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.MovieCategory;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.event.MovieCreatedEvent;
import com.pbl6.cinemate.movie.exception.InternalServerException;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.CategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieActorRepository;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieActorRepository movieActorRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${minio.movie-bucket:}")
    private String movieBucket;

    public MovieServiceImpl(MinioStorageService minio, MovieRepository repo,
            CategoryRepository categoryRepository, MovieCategoryRepository movieCategoryRepository,
            MovieActorRepository movieActorRepository, ApplicationEventPublisher eventPublisher) {
        this.minio = minio;
        this.repo = repo;
        this.categoryRepository = categoryRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieActorRepository = movieActorRepository;
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
        
        // Fetch actors for the movie (only id and fullname)
        List<ActorResponse> actors = movieActorRepository.findByMovieIdWithActor(movieId)
                .stream()
                .map(movieActor -> ActorResponse.builder()
                        .id(movieActor.getActor().getId())
                        .fullname(movieActor.getActor().getFullname())
                        .build())
                .toList();
        
        // Fetch categories for the movie (only id and name)
        List<CategoryResponse> categories = movieCategoryRepository.findByMovieId(movieId)
                .stream()
                .map(movieCategory -> {
                    return categoryRepository.findById(movieCategory.getCategoryId())
                            .map(category -> CategoryResponse.builder()
                                    .id(category.getId())
                                    .name(category.getName())
                                    .build())
                            .orElse(null);
                })
                .filter(category -> category != null)
                .toList();
        
        return MovieUtils.mapToMovieInfoResponse(movie, actors, categories);
    }

    @Override
    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = repo.findAll();
        return movies.stream().map(movie -> {
            String categoryName = getCategoryNameForMovie(movie.getId());
            return MovieUtils.mapToMovieResponse(movie, categoryName);
        }).toList();
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest movieRequest) {
        // Validate that the category exists
        Category category = categoryRepository.findById(movieRequest.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + movieRequest.getCategoryId()));

        // Create and save the movie
        Movie movie = MovieUtils.mapToMovie(movieRequest);
        if (movie == null) {
            throw new InternalServerException("Failed to map MovieRequest to Movie entity");
        }
        Movie savedMovie = repo.save(movie);

        // Create the movie-category relationship
        MovieCategory movieCategory = MovieCategory.builder()
                .movieId(savedMovie.getId())
                .categoryId(category.getId())
                .build();
        movieCategoryRepository.save(movieCategory);

        return MovieUtils.mapToMovieResponse(savedMovie, category.getName());
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(@NonNull UUID movieId, MovieRequest movieRequest) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        // Validate that the category exists
        Category category = categoryRepository.findById(movieRequest.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + movieRequest.getCategoryId()));

        // Update movie fields
        movie.setTitle(movieRequest.getTitle());
        movie.setDescription(movieRequest.getDescription());
        movie.setHorizontalPoster(movieRequest.getHorizontalPoster());
        movie.setVerticalPoster(movieRequest.getVerticalPoster());
        movie.setReleaseDate(movieRequest.getReleaseDate());
        movie.setTrailerUrl(movieRequest.getTrailerUrl());
        movie.setAge(movieRequest.getAge());
        movie.setYear(movieRequest.getYear());
        movie.setCountry(movieRequest.getCountry());
        movie.setIsVip(movieRequest.getIsVip() != null ? movieRequest.getIsVip() : false);

        Movie updatedMovie = repo.save(movie);

        // Update movie-category relationship
        // Delete existing category associations for this movie
        movieCategoryRepository.deleteByMovieId(movieId);
        
        // Create new movie-category relationship
        MovieCategory movieCategory = MovieCategory.builder()
                .movieId(updatedMovie.getId())
                .categoryId(category.getId())
                .build();
        movieCategoryRepository.save(movieCategory);

        return MovieUtils.mapToMovieResponse(updatedMovie, category.getName());
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
        List<MovieResponse> movies = moviePage.getContent().stream().map(movie -> {
            String categoryName = getCategoryNameForMovie(movie.getId());
            return MovieUtils.mapToMovieResponse(movie, categoryName);
        }).toList();
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

    private String getCategoryNameForMovie(UUID movieId) {
        List<MovieCategory> movieCategories = movieCategoryRepository.findByMovieId(movieId);
        if (movieCategories.isEmpty()) {
            return null;
        }
        UUID categoryId = movieCategories.get(0).getCategoryId();
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse(null);
    }

}
