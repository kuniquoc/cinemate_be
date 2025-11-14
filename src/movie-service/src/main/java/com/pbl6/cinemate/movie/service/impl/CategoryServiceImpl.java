package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.CategoryRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.dto.response.CategoryResponse;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;
import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import com.pbl6.cinemate.movie.entity.Category;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.MovieCategory;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.CategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieActorRepository;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieDirectorRepository;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.CategoryService;
import com.pbl6.cinemate.movie.util.MovieUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

        private final CategoryRepository categoryRepository;
        private final MovieCategoryRepository movieCategoryRepository;
        private final MovieRepository movieRepository;
        private final MovieActorRepository movieActorRepository;
        private final MovieDirectorRepository movieDirectorRepository;

        @Transactional(readOnly = true)
        @Override
        public List<CategoryResponse> getAllCategories() {
                return categoryRepository.findAll().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        @Override
        public CategoryResponse getCategoryById(UUID id) {
                Category category = categoryRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
                return mapToResponse(category);
        }

        @Transactional
        @Override
        public CategoryResponse createCategory(CategoryRequest request) {
                if (categoryRepository.existsByName(request.getName())) {
                        throw new NotFoundException("Category already exists with name: " + request.getName());
                }

                Category category = Category.builder()
                                .name(request.getName())
                                .description(request.getDescription())
                                .build();

                Category savedCategory = categoryRepository.save(category);
                return mapToResponse(savedCategory);
        }

        @Transactional
        @Override
        public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
                Category category = categoryRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));

                if (!category.getName().equals(request.getName())
                                && categoryRepository.existsByName(request.getName())) {
                        throw new NotFoundException("Category already exists with name: " + request.getName());
                }

                category.setName(request.getName());
                category.setDescription(request.getDescription());

                Category updatedCategory = categoryRepository.save(category);
                return mapToResponse(updatedCategory);
        }

        @Transactional
        @Override
        public void deleteCategory(UUID id) {
                if (!categoryRepository.existsById(id)) {
                        throw new NotFoundException("Category not found with id: " + id);
                }
                categoryRepository.deleteById(id);
        }

        @Override
        public void addMovieToCategory(UUID categoryId, UUID movieId) {
                Category category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
                Movie movie = movieRepository.findById(movieId)
                                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

                boolean exists = movieCategoryRepository.findByCategoryId(categoryId).stream()
                                .anyMatch(mc -> mc.getMovie().getId().equals(movieId));
                if (exists) {
                        throw new NotFoundException("Movie already in category");
                }

                MovieCategory mc = MovieCategory.builder()
                                .category(category)
                                .movie(movie)
                                .build();
                movieCategoryRepository.save(mc);
        }

        @Transactional
        @Override
        public void removeMovieFromCategory(UUID categoryId, UUID movieId) {
                movieCategoryRepository.deleteByMovieIdAndCategoryId(movieId, categoryId);
        }

        @Override
        public List<MovieResponse> getMoviesByCategory(UUID categoryId) {
                Category category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));

                List<MovieCategory> movieCategories = movieCategoryRepository.findByCategoryId(categoryId);
                if (movieCategories.isEmpty()) {
                        return List.of();
                }

                List<UUID> movieIds = movieCategories.stream()
                                .map(mc -> mc.getMovie().getId())
                                .toList();

                List<Movie> movies = movieRepository.findAllById(movieIds);

                return movies.stream().map(movie -> {
                        // Get categories
                        List<CategoryResponse> categoryResponses = movieCategoryRepository
                                        .findByMovieIdWithCategory(movie.getId())
                                        .stream()
                                        .map(mc -> CategoryResponse.builder()
                                                        .id(mc.getCategory().getId())
                                                        .name(mc.getCategory().getName())
                                                        .build())
                                        .toList();

                        // Get actors
                        List<ActorResponse> actorResponses = movieActorRepository.findByMovieIdWithActor(movie.getId())
                                        .stream()
                                        .map(ma -> ActorResponse.builder()
                                                        .id(ma.getActor().getId())
                                                        .fullname(ma.getActor().getFullname())
                                                        .build())
                                        .toList();

                        // Get directors
                        List<DirectorResponse> directorResponses = movieDirectorRepository
                                        .findByMovieIdWithDirector(movie.getId())
                                        .stream()
                                        .map(md -> DirectorResponse.builder()
                                                        .id(md.getDirector().getId())
                                                        .fullname(md.getDirector().getFullname())
                                                        .build())
                                        .toList();

                        return MovieUtils.mapToMovieResponse(movie, categoryResponses, actorResponses,
                                        directorResponses);
                }).toList();
        }

        private CategoryResponse mapToResponse(Category category) {
                return CategoryResponse.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .description(category.getDescription())
                                .build();
        }
}