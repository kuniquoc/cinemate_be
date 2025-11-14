package com.pbl6.cinemate.movie.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.dto.request.DirectorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.DirectorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.DirectorResponse;
import com.pbl6.cinemate.movie.service.DirectorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/directors")
@Tag(name = "Director Management", description = "Director creation and management")
public class DirectorController {

    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @Operation(summary = "Create director", description = "Create a new director with the provided information")
    @PostMapping
    public ResponseEntity<ResponseData> createDirector(
            @Valid @RequestBody DirectorCreationRequest request,
            HttpServletRequest httpServletRequest) {

        DirectorResponse response = directorService.createDirector(request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Director created successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get all directors", description = "Retrieve a list of all directors")
    @GetMapping
    public ResponseEntity<ResponseData> getAllDirectors(HttpServletRequest httpServletRequest) {

        List<DirectorResponse> response = directorService.getAllDirectors();

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Directors retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get director by ID", description = "Retrieve a specific director by their ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getDirectorById(
            @Parameter(description = "Director ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        DirectorResponse response = directorService.getDirectorById(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Director retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Update director", description = "Update an existing director's information")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponseData> updateDirector(
            @Parameter(description = "Director ID") @PathVariable UUID id,
            @Valid @RequestBody DirectorUpdateRequest request,
            HttpServletRequest httpServletRequest) {

        DirectorResponse response = directorService.updateDirector(id, request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Director updated successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
