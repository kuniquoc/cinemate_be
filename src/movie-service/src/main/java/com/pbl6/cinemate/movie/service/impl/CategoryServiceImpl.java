package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.CategoryRequest;
import com.pbl6.cinemate.movie.dto.response.CategoryResponse;
import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import com.pbl6.cinemate.movie.entity.Category;
import com.pbl6.cinemate.movie.entity.MovieCategory;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.CategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
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
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found with id: " + categoryId);
        }
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found with id: " + movieId);
        }
        boolean exists = movieCategoryRepository.findByCategoryId(categoryId).stream()
                .anyMatch(mc -> mc.getMovieId().equals(movieId));
        if (exists) {
            throw new NotFoundException("Movie already in category");
        }
        MovieCategory mc = MovieCategory.builder()
                .categoryId(categoryId)
                .movieId(movieId)
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
        List<MovieCategory> movieCategories = movieCategoryRepository.findByCategoryId(categoryId);
        List<UUID> movieIds = movieCategories.stream().map(MovieCategory::getMovieId).toList();
        return movieRepository.findAllById(movieIds).stream().map(MovieUtils::mapToMovieResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}