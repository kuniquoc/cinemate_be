package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.ContentAccessRequest;
import com.pbl6.cinemate.payment_service.dto.response.ContentAccessResponse;
import com.pbl6.cinemate.payment_service.service.ContentAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/content-access")
@RequiredArgsConstructor
@Slf4j
public class ContentAccessController {
    
    private final ContentAccessService contentAccessService;
    
    /**
     * Check if a user has permission to watch content based on subscription and parental controls
     * 
     * @param request Contains userId, movieCategories, and currentWatchTimeMinutes
     * @return ContentAccessResponse with allowed status and additional information
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkContentAccess(
            @Valid @RequestBody ContentAccessRequest request) {
        
        log.info("Content access check request received for user: {}", request.getUserId());
        
        ContentAccessResponse response = contentAccessService.checkContentAccess(
                request.getUserId(),
                request.getMovieCategories(),
                request.getCurrentWatchTimeMinutes()
        );
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "success");
        responseBody.put("data", response);
        
        log.info("Content access check completed for user: {}, allowed: {}", 
                request.getUserId(), response.getAllowed());
        
        return ResponseEntity.ok(responseBody);
    }
}
