package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.request.WatchProgressRequest;
import com.pbl6.cinemate.movie.dto.response.WatchHistoryDateResponse;
import com.pbl6.cinemate.movie.dto.response.WatchHistoryResponse;
import com.pbl6.cinemate.movie.service.WatchHistoryService;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Watch History", description = "Watch history management for tracking movie viewing progress")
public class WatchHistoryController {

        private final WatchHistoryService watchHistoryService;

        @Operation(summary = "Save watch progress", description = "Save or update watch progress when user stops watching a movie")
        @PostMapping("/{movieId}/watch-progress")
        public ResponseEntity<ResponseData> saveWatchProgress(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @CurrentUser UserPrincipal userPrincipal,
                        @Valid @RequestBody WatchProgressRequest request,
                        HttpServletRequest httpServletRequest) {

                watchHistoryService.saveWatchProgress(movieId, userPrincipal.getId(), request);

                return ResponseEntity.ok(ResponseData.success(
                                null,
                                "Watch progress saved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get watch history dates", description = "Get paginated list of dates when user watched movies")
        @GetMapping("/watch-history/dates")
        public ResponseEntity<ResponseData> getWatchHistoryDates(
                        @CurrentUser UserPrincipal userPrincipal,
                        @Parameter(description = "Page number (1-indexed)") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        HttpServletRequest httpServletRequest) {

                PaginatedResponse<WatchHistoryDateResponse> response = watchHistoryService.getWatchHistoryDates(
                                userPrincipal.getId(), page - 1, size);

                return ResponseEntity.ok(ResponseData.successWithMeta(
                                response,
                                "Watch history dates retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get watch history by date", description = "Get paginated list of movies watched on a specific date")
        @GetMapping("/watch-history")
        public ResponseEntity<ResponseData> getWatchHistoryByDate(
                        @CurrentUser UserPrincipal userPrincipal,
                        @Parameter(description = "Date in ISO format (yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @Parameter(description = "Page number (1-indexed)") @RequestParam(defaultValue = "1") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
                        HttpServletRequest httpServletRequest) {

                PaginatedResponse<WatchHistoryResponse> response = watchHistoryService.getWatchHistoryByDate(
                                userPrincipal.getId(), date, page - 1, size);

                return ResponseEntity.ok(ResponseData.successWithMeta(
                                response,
                                "Watch history retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Delete watch history", description = "Delete watch history for a specific movie")
        @DeleteMapping("/{movieId}/watch-progress")
        public ResponseEntity<ResponseData> deleteWatchHistory(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {

                watchHistoryService.deleteWatchHistory(movieId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseData.success(
                                null,
                                "Watch history deleted successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get watch progress for movie", description = "Get last watched position and total duration for a movie for current user")
        @GetMapping("/{movieId}/watch-progress")
        public ResponseEntity<ResponseData> getWatchProgress(
                        @Parameter(description = "Movie ID") @PathVariable UUID movieId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpServletRequest) {

                var response = watchHistoryService.getWatchProgress(movieId, userPrincipal.getId());

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Watch progress retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }
}
