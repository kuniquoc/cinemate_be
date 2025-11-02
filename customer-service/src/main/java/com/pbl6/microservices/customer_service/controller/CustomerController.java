package com.pbl6.microservices.customer_service.controller;

import com.pbl6.microservices.customer_service.constants.FeedbackMessage;
import com.pbl6.microservices.customer_service.payload.general.ResponseData;
import com.pbl6.microservices.customer_service.payload.request.FavoriteCreateRequest;
import com.pbl6.microservices.customer_service.payload.request.UpdateProfileRequest;
import com.pbl6.microservices.customer_service.payload.response.FavoriteResponse;
import com.pbl6.microservices.customer_service.security.UserPrincipal;
import com.pbl6.microservices.customer_service.security.annotation.CurrentUser;
import com.pbl6.microservices.customer_service.service.CustomerService;
import com.pbl6.microservices.customer_service.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final FavoriteService favoriteService;

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
        ResponseData responseData = ResponseData.success(customerService.updateProfile(userPrincipal.getUserId(),
                        updateProfileRequest),
                FeedbackMessage.PROFILE_UPDATED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PostMapping("/favorites")
    public ResponseEntity<ResponseData> addFavorite(@CurrentUser UserPrincipal userPrincipal,
                                                    @Valid @RequestBody FavoriteCreateRequest request,
                                                    HttpServletRequest httpRequest) {
        FavoriteResponse response = favoriteService.addFavorite(userPrincipal.getUserId(), request);
        return ResponseEntity.ok(ResponseData.success(response, "Favorite added", httpRequest.getRequestURI(), httpRequest.getMethod()));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ResponseData> getFavorites(@CurrentUser UserPrincipal userPrincipal,
                                                     HttpServletRequest httpRequest) {
        List<FavoriteResponse> favorites = favoriteService.getFavorites(userPrincipal.getUserId());
        return ResponseEntity.ok(ResponseData.success(favorites, "Favorites fetched", httpRequest.getRequestURI(), httpRequest.getMethod()));
    }

    @DeleteMapping("/favorites/{movieId}")
    public ResponseEntity<ResponseData> removeFavorite(@CurrentUser UserPrincipal userPrincipal,
                                                       @PathVariable UUID movieId,
                                                       HttpServletRequest httpRequest) {
        favoriteService.removeFavorite(userPrincipal.getUserId(), movieId);
        return ResponseEntity.ok(ResponseData.success(null, "Favorite removed", httpRequest.getRequestURI(), httpRequest.getMethod()));
    }

}
