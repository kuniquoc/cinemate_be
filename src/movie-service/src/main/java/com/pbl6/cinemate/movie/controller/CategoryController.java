package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.request.CategoryRequest;
import com.pbl6.cinemate.movie.service.CategoryService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ResponseData> getAllCategories(HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(categoryService.getAllCategories(),
                "get categories successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getCategoryById(@PathVariable UUID id, HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(categoryService.getCategoryById(id),
                "get category by id successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> createCategory(
            @Valid @RequestBody CategoryRequest categoryRequest, HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(categoryService.createCategory(categoryRequest),
                "create category by id successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responseData);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest categoryRequest, HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(categoryService.updateCategory(id, categoryRequest),
                "update category by id successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> deleteCategory(@PathVariable UUID id, HttpServletRequest request) {
        categoryService.deleteCategory(id);
        ResponseData responseData = ResponseData.success(
                "delete category by id successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/{categoryId}/movies/{movieId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> addMovieToCategory(@PathVariable UUID categoryId,
                                                           @PathVariable UUID movieId,
                                                           HttpServletRequest request) {
        categoryService.addMovieToCategory(categoryId, movieId);
        ResponseData responseData = ResponseData.success(
                "add movie to category successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping("/{categoryId}/movies/{movieId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> removeMovieFromCategory(@PathVariable UUID categoryId,
                                                                @PathVariable UUID movieId,
                                                                HttpServletRequest request) {
        categoryService.removeMovieFromCategory(categoryId, movieId);
        ResponseData responseData = ResponseData.success(
                "remove movie from category successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @GetMapping("/{categoryId}/movies")
    public ResponseEntity<ResponseData> getMoviesByCategory(@PathVariable UUID categoryId,
                                                            HttpServletRequest request) {
        ResponseData responseData = ResponseData.success(categoryService.getMoviesByCategory(categoryId),
                "get movies by category successfully",
                request.getRequestURI(),
                request.getMethod());
        return ResponseEntity.ok(responseData);
    }
}