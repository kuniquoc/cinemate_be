package com.pbl6.cinemate.movie.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pbl6.cinemate.movie.dto.request.ActorCreationRequest;
import com.pbl6.cinemate.movie.dto.request.ActorUpdateRequest;
import com.pbl6.cinemate.movie.dto.response.ActorResponse;
import com.pbl6.cinemate.movie.service.ActorService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/actors")
@Tag(name = "Actor Management", description = "Actor creation and management")
public class ActorController {

    private final ActorService actorService;

    public ActorController(ActorService actorService) {
        this.actorService = actorService;
    }

    @Operation(summary = "Create actor", description = "Create a new actor with the provided information (Admin only)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> createActor(
            @Valid @RequestBody ActorCreationRequest request,
            HttpServletRequest httpServletRequest) {

        ActorResponse response = actorService.createActor(request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Actor created successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get all actors", description = "Retrieve a list of all actors")
    @GetMapping
    public ResponseEntity<ResponseData> getAllActors(HttpServletRequest httpServletRequest) {

        List<ActorResponse> response = actorService.getAllActors();

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Actors retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Get actor by ID", description = "Retrieve a specific actor by their ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getActorById(
            @Parameter(description = "Actor ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        ActorResponse response = actorService.getActorById(id);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Actor retrieved successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Update actor", description = "Update an existing actor's information (Admin only)")
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> updateActor(
            @Parameter(description = "Actor ID") @PathVariable UUID id,
            @Valid @RequestBody ActorUpdateRequest request,
            HttpServletRequest httpServletRequest) {

        ActorResponse response = actorService.updateActor(id, request);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Actor updated successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }

    @Operation(summary = "Delete actor", description = "Delete an actor by their ID (Admin only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseData> deleteActor(
            @Parameter(description = "Actor ID") @PathVariable UUID id,
            HttpServletRequest httpServletRequest) {

        actorService.deleteActor(id);

        return ResponseEntity.ok(ResponseData.success(
                null,
                "Actor deleted successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
