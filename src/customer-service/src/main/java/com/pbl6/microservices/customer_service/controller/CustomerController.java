package com.pbl6.microservices.customer_service.controller;

import com.pbl6.microservices.customer_service.client.dto.MovieDetailResponse;
import com.pbl6.microservices.customer_service.constants.FeedbackMessage;
import com.pbl6.microservices.customer_service.payload.general.PageMeta;
import com.pbl6.microservices.customer_service.payload.general.ResponseData;
import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;
import com.pbl6.microservices.customer_service.payload.response.ImageUploadResponse;
import com.pbl6.microservices.customer_service.security.UserPrincipal;
import com.pbl6.microservices.customer_service.security.annotation.CurrentUser;
import com.pbl6.microservices.customer_service.service.CustomerService;
import com.pbl6.microservices.customer_service.service.FavoriteService;
import com.pbl6.microservices.customer_service.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

        private final CustomerService customerService;
        private final FavoriteService favoriteService;
        private final StorageService storageService;

        @GetMapping("/profile")
        public ResponseEntity<ResponseData> getProfile(@CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest request) {
                ResponseData responseData = ResponseData.success(customerService.getProfile(userPrincipal.getUserId()),
                                FeedbackMessage.PROFILE_FETCHED, request.getRequestURI(), request.getMethod());
                return ResponseEntity.ok(responseData);
        }

        @PatchMapping("/profile")
        public ResponseEntity<ResponseData> updateProfile(@CurrentUser UserPrincipal userPrincipal,
                        @Valid @RequestBody UpdateProfileRequest updateProfileRequest,
                        HttpServletRequest request) {
                ResponseData responseData = ResponseData.success(
                                customerService.updateProfile(userPrincipal.getUserId(),
                                                updateProfileRequest),
                                FeedbackMessage.PROFILE_UPDATED, request.getRequestURI(), request.getMethod());
                return ResponseEntity.ok(responseData);
        }

        @PostMapping("/upload-image")
        public ResponseEntity<ResponseData> uploadImage(@CurrentUser UserPrincipal userPrincipal,
                        @RequestParam("file") MultipartFile file,
                        HttpServletRequest request) {
                String imageUrl = storageService.uploadImage(file);
                ImageUploadResponse response = ImageUploadResponse.builder()
                                .imageUrl(imageUrl)
                                .build();
                ResponseData responseData = ResponseData.success(response,
                                FeedbackMessage.IMAGE_UPLOADED, request.getRequestURI(), request.getMethod());
                return ResponseEntity.ok(responseData);
        }

        @PostMapping("/favorites")
        public ResponseEntity<ResponseData> addFavorite(@CurrentUser UserPrincipal userPrincipal,
                        @Valid @RequestBody FavoriteCreateRequest request,
                        HttpServletRequest httpRequest) {
                FavoriteResponse response = favoriteService.addFavorite(userPrincipal.getUserId(), request);
                return ResponseEntity.ok(ResponseData.success(response, "Favorite added", httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/favorites")
        public ResponseEntity<ResponseData> getFavorites(@CurrentUser UserPrincipal userPrincipal,
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "limit", defaultValue = "10") int limit,
                        HttpServletRequest httpRequest) {
                Page<MovieDetailResponse> favoritePage = favoriteService.getFavorites(userPrincipal.getUserId(), page,
                                limit);

                PageMeta pageMeta = PageMeta.builder()
                                .limit(limit)
                                .currentPage(page)
                                .totalPage(favoritePage.getTotalPages())
                                .build();

                return ResponseEntity.ok(ResponseData.successWithMeta(
                                favoritePage.getContent(),
                                pageMeta,
                                "Favorites fetched",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @DeleteMapping("/favorites/{movieId}")
        public ResponseEntity<ResponseData> removeFavorite(@CurrentUser UserPrincipal userPrincipal,
                        @PathVariable UUID movieId,
                        HttpServletRequest httpRequest) {
                favoriteService.removeFavorite(userPrincipal.getUserId(), movieId);
                return ResponseEntity.ok(ResponseData.success(null, "Favorite removed", httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

}
