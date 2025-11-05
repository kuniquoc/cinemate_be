package com.pbl6.cinemate.movie.service;

import com.pbl6.cinemate.movie.dto.request.CategoryRequest;
import com.pbl6.cinemate.movie.dto.response.CategoryResponse;
import com.pbl6.cinemate.movie.dto.response.MovieResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    @Transactional(readOnly = true)
    List<CategoryResponse> getAllCategories();

    @Transactional(readOnly = true)
    CategoryResponse getCategoryById(UUID id);

    @Transactional
    CategoryResponse createCategory(CategoryRequest request);

    @Transactional
    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    @Transactional
    void deleteCategory(UUID id);

    void addMovieToCategory(UUID categoryId, UUID movieId);

    void removeMovieFromCategory(UUID categoryId, UUID movieId);

    List<MovieResponse> getMoviesByCategory(UUID categoryId);
}
