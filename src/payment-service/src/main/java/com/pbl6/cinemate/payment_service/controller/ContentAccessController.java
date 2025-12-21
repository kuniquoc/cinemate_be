package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.ContentAccessRequest;
import com.pbl6.cinemate.payment_service.dto.response.ContentAccessResponse;
import com.pbl6.cinemate.payment_service.service.ContentAccessService;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ContentAccessController {

        private final ContentAccessService contentAccessService;

        /**
         * Check if a user has permission to watch content based on subscription and
         * parental controls
         *
         * @param request       Contains movieCategories and currentWatchTimeMinutes
         * @param userPrincipal Authenticated user from JWT token
         * @return ContentAccessResponse with allowed status and additional information
         */
        @PostMapping("/api/v1/content-access/check")
        public ResponseEntity<Map<String, Object>> checkContentAccess(
                        @Valid @RequestBody ContentAccessRequest request,
                        @CurrentUser UserPrincipal userPrincipal) {

                UUID userId = userPrincipal.getId();
                log.info("Content access check request received for user: {}", userId);

                ContentAccessResponse response = contentAccessService.checkContentAccess(
                                userId,
                                request.getMovieCategoryIds(),
                                request.getCurrentWatchTimeMinutes());

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("status", "success");
                responseBody.put("data", response);

                log.info("Content access check completed for user: {}, allowed: {}",
                                userId, response.getAllowed());

                return ResponseEntity.ok(responseBody);
        }

        /**
         * Internal endpoint for content access check (used by other services)
         * This endpoint does not require authentication and accepts userId as a parameter
         *
         * @param userId                   The user ID to check
         * @param movieCategoryIds         List of movie category IDs
         * @param currentWatchTimeMinutes Watch time in minutes
         * @return ContentAccessResponse with allowed status and additional information
         */
        @PostMapping("/api/internal/content-access/check")
        public ResponseEntity<Map<String, Object>> checkContentAccessInternal(
                        @RequestParam UUID userId,
                        @RequestParam List<UUID> movieCategoryIds,
                        @RequestParam Integer currentWatchTimeMinutes) {

                log.info("Internal content access check request received for user: {}", userId);

                ContentAccessResponse response = contentAccessService.checkContentAccess(
                                userId,
                                movieCategoryIds,
                                currentWatchTimeMinutes);

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("status", "success");
                responseBody.put("data", response);

                log.info("Internal content access check completed for user: {}, allowed: {}",
                                userId, response.getAllowed());

                return ResponseEntity.ok(responseBody);
        }
}
