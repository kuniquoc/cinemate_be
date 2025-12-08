package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.MovieRequest;
import com.pbl6.cinemate.movie.dto.request.MovieUploadRequest;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.entity.*;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.enums.MovieProcessStatus;
import com.pbl6.cinemate.movie.event.MovieCreatedEvent;
import com.pbl6.cinemate.movie.repository.*;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.MovieService;
import com.pbl6.cinemate.movie.service.WatchHistoryService;
import com.pbl6.cinemate.movie.util.MovieUtils;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.InternalServerException;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class MovieServiceImpl implements MovieService {
    private final MinioStorageService minio;
    private final MovieRepository repo;
    private final CategoryRepository categoryRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieActorRepository movieActorRepository;
    private final MovieDirectorRepository movieDirectorRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WatchHistoryService watchHistoryService;

    @Value("${minio.movie-bucket:}")
    private String movieBucket;

    public MovieServiceImpl(MinioStorageService minio, MovieRepository repo,
            CategoryRepository categoryRepository, MovieCategoryRepository movieCategoryRepository,
            MovieActorRepository movieActorRepository, MovieDirectorRepository movieDirectorRepository,
            ActorRepository actorRepository, DirectorRepository directorRepository,
            ApplicationEventPublisher eventPublisher, @Lazy WatchHistoryService watchHistoryService) {
        this.minio = minio;
        this.repo = repo;
        this.categoryRepository = categoryRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieActorRepository = movieActorRepository;
        this.movieDirectorRepository = movieDirectorRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.eventPublisher = eventPublisher;
        this.watchHistoryService = watchHistoryService;
    }

    @Override
    @Transactional
    public MovieUploadResponse upload(MultipartFile file, MovieUploadRequest req) {
        Movie movie = repo.save(Movie.builder()
                .title(req.title())
                .description(req.description())
                .build());

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
        List<String> qualities = movie.getQualities() != null ? movie.getQualities() : List.of();
        return new MovieStatusResponse(movie.getId(), movie.getStatus().name(), qualities);
    }

    @Override
    public MovieInfoResponse getMovieInfo(@NonNull UUID movieId, UUID customerId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        // Fetch actors for the movie
        List<ActorResponse> actors = movieActorRepository.findByMovieIdWithActor(movieId)
                .stream()
                .map(movieActor -> ActorResponse.builder()
                        .id(movieActor.getActor().getId())
                        .fullname(movieActor.getActor().getFullname())
                        .biography(movieActor.getActor().getBiography())
                        .avatar(movieActor.getActor().getAvatar())
                        .dateOfBirth(movieActor.getActor().getDateOfBirth())
                        .build())
                .toList();

        // Fetch directors for the movie
        List<DirectorResponse> directors = movieDirectorRepository.findByMovieIdWithDirector(movieId)
                .stream()
                .map(movieDirector -> DirectorResponse.builder()
                        .id(movieDirector.getDirector().getId())
                        .fullname(movieDirector.getDirector().getFullname())
                        .biography(movieDirector.getDirector().getBiography())
                        .avatar(movieDirector.getDirector().getAvatar())
                        .dateOfBirth(movieDirector.getDirector().getDateOfBirth())
                        .build())
                .toList();

        // Fetch categories for the movie
        List<CategoryResponse> categories = movieCategoryRepository.findByMovieIdWithCategory(movieId)
                .stream()
                .map(movieCategory -> CategoryResponse.builder()
                        .id(movieCategory.getCategory().getId())
                        .name(movieCategory.getCategory().getName())
                        .build())
                .toList();

        // Get last watched position if customer is logged in
        Long lastWatchedPosition = watchHistoryService.getLastWatchedPosition(movieId, customerId);

        return MovieUtils.mapToMovieInfoResponse(movie, actors, directors, categories, lastWatchedPosition);
    }

    @Override
    public List<MovieResponse> getAllMovies() {
        return repo.findAll().stream().map(movie -> {
            List<CategoryResponse> categories = getCategoriesForMovie(movie.getId());
            List<ActorResponse> actors = getActorsForMovie(movie.getId());
            List<DirectorResponse> directors = getDirectorsForMovie(movie.getId());
            return MovieUtils.mapToMovieResponse(movie, categories, actors, directors);
        }).toList();
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest movieRequest) {
        // Validate and fetch categories
        List<UUID> categoryIds = movieRequest.getCategoryIds();
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("At least one category ID must be provided");
        }
        List<Category> categories = categoryRepository.findAllByIdIn(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new NotFoundException("Some category IDs were not found");
        }

        // Validate and fetch actors (optional)
        List<UUID> actorIds = movieRequest.getActorIds();
        List<Actor> actors = List.of();
        if (actorIds != null && !actorIds.isEmpty()) {
            actors = actorRepository.findAllById(actorIds);
            if (actors.size() != actorIds.size()) {
                throw new NotFoundException("Some actor IDs were not found");
            }
        }

        // Validate and fetch directors (optional)
        List<UUID> directorIds = movieRequest.getDirectorIds();
        List<Director> directors = List.of();
        if (directorIds != null && !directorIds.isEmpty()) {
            directors = directorRepository.findAllById(directorIds);
            if (directors.size() != directorIds.size()) {
                throw new NotFoundException("Some director IDs were not found");
            }
        }

        // Create and save movie
        Movie movie = MovieUtils.mapToMovie(movieRequest);
        if (movie == null) {
            throw new InternalServerException("Failed to map MovieRequest to Movie entity");
        }
        Movie savedMovie = repo.save(movie);

        // Create MovieCategory relationships
        List<MovieCategory> movieCategories = categories.stream()
                .map(category -> new MovieCategory(savedMovie, category))
                .toList();
        movieCategoryRepository.saveAll(movieCategories);

        // Create MovieActor relationships
        List<MovieActor> movieActors = actors.stream()
                .map(actor -> new MovieActor(savedMovie, actor))
                .toList();
        movieActorRepository.saveAll(movieActors);

        // Create MovieDirector relationships
        List<MovieDirector> movieDirectors = directors.stream()
                .map(director -> new MovieDirector(savedMovie, director))
                .toList();
        movieDirectorRepository.saveAll(movieDirectors);

        // Build responses
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .toList();

        List<ActorResponse> actorResponses = actors.stream()
                .map(actor -> ActorResponse.builder()
                        .id(actor.getId())
                        .fullname(actor.getFullname())
                        .build())
                .toList();

        List<DirectorResponse> directorResponses = directors.stream()
                .map(director -> DirectorResponse.builder()
                        .id(director.getId())
                        .fullname(director.getFullname())
                        .build())
                .toList();

        return MovieUtils.mapToMovieResponse(savedMovie, categoryResponses, actorResponses, directorResponses);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(@NonNull UUID movieId, MovieRequest movieRequest) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        // Validate and fetch categories
        List<UUID> categoryIds = movieRequest.getCategoryIds();
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("At least one category ID must be provided");
        }
        List<Category> categories = categoryRepository.findAllByIdIn(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new NotFoundException("Some category IDs were not found");
        }

        // Validate and fetch actors (optional)
        List<UUID> actorIds = movieRequest.getActorIds();
        List<Actor> actors = List.of();
        if (actorIds != null && !actorIds.isEmpty()) {
            actors = actorRepository.findAllById(actorIds);
            if (actors.size() != actorIds.size()) {
                throw new NotFoundException("Some actor IDs were not found");
            }
        }

        // Validate and fetch directors (optional)
        List<UUID> directorIds = movieRequest.getDirectorIds();
        List<Director> directors = List.of();
        if (directorIds != null && !directorIds.isEmpty()) {
            directors = directorRepository.findAllById(directorIds);
            if (directors.size() != directorIds.size()) {
                throw new NotFoundException("Some director IDs were not found");
            }
        }

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
        movie.setIsVip(Boolean.TRUE.equals(movieRequest.getIsVip()));

        Movie updatedMovie = repo.save(movie);

        // Update categories - delete existing and flush before adding new ones
        movieCategoryRepository.deleteByMovieId(movieId);
        movieCategoryRepository.flush();
        List<MovieCategory> movieCategories = categories.stream()
                .map(category -> new MovieCategory(updatedMovie, category))
                .toList();
        movieCategoryRepository.saveAll(movieCategories);

        // Update actors - delete existing and flush before adding new ones
        movieActorRepository.deleteByMovieId(movieId);
        movieActorRepository.flush();
        List<MovieActor> movieActors = actors.stream()
                .map(actor -> new MovieActor(updatedMovie, actor))
                .toList();
        movieActorRepository.saveAll(movieActors);

        // Update directors - delete existing and flush before adding new ones
        movieDirectorRepository.deleteByMovieId(movieId);
        movieDirectorRepository.flush();
        List<MovieDirector> movieDirectors = directors.stream()
                .map(director -> new MovieDirector(updatedMovie, director))
                .toList();
        movieDirectorRepository.saveAll(movieDirectors);

        // Build responses
        List<CategoryResponse> categoryResponses = categories.stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .toList();

        List<ActorResponse> actorResponses = actors.stream()
                .map(actor -> ActorResponse.builder()
                        .id(actor.getId())
                        .fullname(actor.getFullname())
                        .build())
                .toList();

        List<DirectorResponse> directorResponses = directors.stream()
                .map(director -> DirectorResponse.builder()
                        .id(director.getId())
                        .fullname(director.getFullname())
                        .build())
                .toList();

        return MovieUtils.mapToMovieResponse(updatedMovie, categoryResponses, actorResponses, directorResponses);
    }

    @Override
    @Transactional
    public void deleteMovie(@NonNull UUID movieId) {
        if (!repo.existsById(movieId)) {
            throw new NotFoundException("Movie not found");
        }

        // Delete all relationships first to avoid ConcurrentModificationException
        movieCategoryRepository.deleteByMovieId(movieId);
        movieActorRepository.deleteByMovieId(movieId);
        movieDirectorRepository.deleteByMovieId(movieId);

        // Then delete the movie
        repo.deleteById(movieId);
    }

    @Override
    public PaginatedResponse<MovieResponse> getMovies(String keyword, int page, int size, String sortBy,
            @NonNull String sortDirection, String userRole) {
        // TODO: Replace userRole parameter with proper authentication/authorization
        // after implementing security for movie-service
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        // If user role is ADMIN, pass null to get all movies; otherwise pass "PUBLIC"
        // to get only public movies
        MovieStatus statusFilter = "ADMIN".equalsIgnoreCase(userRole) ? null : MovieStatus.PUBLIC;

        // Use different queries based on whether keyword exists to avoid unnecessary
        // JOINs
        Page<Movie> moviePage;
        if (keyword != null && !keyword.isBlank()) {
            // Search with JOINs when keyword is provided
            moviePage = repo.searchMoviesByKeyword(keyword, statusFilter, pageable);
        } else {
            // Simple query without JOINs when no keyword
            moviePage = repo.findAllByStatus(statusFilter, pageable);
        }

        List<MovieResponse> movies = moviePage.getContent().stream().map(movie -> {
            List<CategoryResponse> categories = getCategoriesForMovie(movie.getId());
            List<ActorResponse> actors = getActorsForMovie(movie.getId());
            List<DirectorResponse> directors = getDirectorsForMovie(movie.getId());
            return MovieUtils.mapToMovieResponse(movie, categories, actors, directors);
        }).toList();
        return new PaginatedResponse<>(movies, moviePage.getNumber(), moviePage.getSize(), moviePage.getTotalPages());
    }

    @Override
    public List<MovieResponse> getTopTenMovies() {
        var pageable = PageRequest.of(0, 10);
        List<Movie> topMovies = repo.findTop10ByOrderByRankAsc(pageable);
        return topMovies.stream().map(movie -> {
            List<CategoryResponse> categories = getCategoriesForMovie(movie.getId());
            List<ActorResponse> actors = getActorsForMovie(movie.getId());
            List<DirectorResponse> directors = getDirectorsForMovie(movie.getId());
            return MovieUtils.mapToMovieResponse(movie, categories, actors, directors);
        }).toList();
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

    private List<CategoryResponse> getCategoriesForMovie(UUID movieId) {
        return movieCategoryRepository.findByMovieIdWithCategory(movieId)
                .stream()
                .map(mc -> CategoryResponse.builder()
                        .id(mc.getCategory().getId())
                        .name(mc.getCategory().getName())
                        .build())
                .toList();
    }

    private List<ActorResponse> getActorsForMovie(UUID movieId) {
        return movieActorRepository.findByMovieIdWithActor(movieId)
                .stream()
                .map(ma -> ActorResponse.builder()
                        .id(ma.getActor().getId())
                        .fullname(ma.getActor().getFullname())
                        .build())
                .toList();
    }

    private List<DirectorResponse> getDirectorsForMovie(UUID movieId) {
        return movieDirectorRepository.findByMovieIdWithDirector(movieId)
                .stream()
                .map(md -> DirectorResponse.builder()
                        .id(md.getDirector().getId())
                        .fullname(md.getDirector().getFullname())
                        .build())
                .toList();
    }

    @Override
    public MovieProcessStatusResponse getMovieProcessStatus(@NonNull UUID movieId) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));
        return new MovieProcessStatusResponse(movie.getId(),
                movie.getProcessStatus() != null ? movie.getProcessStatus().name() : null);
    }

    @Override
    @Transactional
    public MovieResponse updateMovieStatus(@NonNull UUID movieId, @NonNull String newStatusString) {
        var newStatus = MovieStatus.valueOf(newStatusString);

        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        // Reject DRAFT status updates
        if (movie.getStatus() == MovieStatus.DRAFT) {
            throw new BadRequestException("Cannot update status of a DRAFT movie, upload a new movie instead");
        }

        // Only allow PRIVATE or PUBLIC
        if (newStatus != MovieStatus.PRIVATE && newStatus != MovieStatus.PUBLIC) {
            throw new BadRequestException("Status can only be updated to PRIVATE or PUBLIC");
        }

        // Check that transcoding is completed before allowing status change
        if (movie.getProcessStatus() != MovieProcessStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot update movie status. Movie processing is not completed yet. Current process status: "
                            + movie.getProcessStatus().name());
        }

        // Update status
        movie.setStatus(newStatus);
        Movie updatedMovie = repo.save(movie);

        // Return movie response with full details
        List<CategoryResponse> categories = getCategoriesForMovie(movieId);
        List<ActorResponse> actors = getActorsForMovie(movieId);
        List<DirectorResponse> directors = getDirectorsForMovie(movieId);

        return MovieUtils.mapToMovieResponse(updatedMovie, categories, actors, directors);
    }

}
