package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.entity.Category;
import com.pbl6.cinemate.movie.entity.MovieCategory;
import com.pbl6.cinemate.movie.repository.CategoryRepository;
import com.pbl6.cinemate.movie.repository.MovieCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal API controller for inter-service communication
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalMovieController {

    private final MovieCategoryRepository movieCategoryRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Get all category IDs for a specific movie (for content access check)
     *
     * @param movieId The movie ID
     * @return List of category UUIDs
     */
    @GetMapping("/movies/{movieId}/category-ids")
    public List<UUID> getMovieCategoryIds(@PathVariable UUID movieId) {
        log.info("Fetching category IDs for movie: {}", movieId);
        
        List<UUID> categoryIds = movieCategoryRepository.findByMovieIdWithCategory(movieId)
                .stream()
                .map(movieCategory -> movieCategory.getCategory().getId())
                .collect(Collectors.toList());
        
        log.info("Found {} categories for movie: {}", categoryIds.size(), movieId);
        return categoryIds;
    }

    /**
     * Get category names by their IDs (for error messages)
     *
     * @param categoryIds List of category UUIDs
     * @return Map of UUID string to category name
     */
    @GetMapping("/categories/names")
    public Map<String, String> getCategoryNames(@RequestParam("ids") List<UUID> categoryIds) {
        log.info("Fetching category names for {} categories", categoryIds.size());
        
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        
        Map<String, String> categoryNamesMap = new HashMap<>();
        for (Category category : categories) {
            categoryNamesMap.put(category.getId().toString(), category.getName());
        }
        
        log.info("Found {} category names", categoryNamesMap.size());
        return categoryNamesMap;
    }
}
