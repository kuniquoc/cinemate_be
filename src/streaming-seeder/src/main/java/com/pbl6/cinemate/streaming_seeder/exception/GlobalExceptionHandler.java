package com.pbl6.cinemate.streaming_seeder.exception;

import com.pbl6.cinemate.shared.dto.general.ErrorResponse;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for streaming-seeder service
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle content access denied exceptions
     */
    @ExceptionHandler(ContentAccessDeniedException.class)
    public ResponseEntity<ResponseData> handleContentAccessDenied(
            ContentAccessDeniedException ex,
            WebRequest request) {
        
        log.warn("Content access denied: {}", ex.getReason());
        
        // Build message with additional info for kids
        String message = ex.getReason();
        if (Boolean.TRUE.equals(ex.getIsKid())) {
            if (ex.getBlockedCategoryNames() != null && !ex.getBlockedCategoryNames().isEmpty()) {
                message += ". Blocked categories: " + String.join(", ", ex.getBlockedCategoryNames());
            }
            if (ex.getRemainingWatchTimeMinutes() != null) {
                message += ". Remaining watch time: " + ex.getRemainingWatchTimeMinutes() + " minutes";
            }
        }
        
        ResponseData responseData = ResponseData.builder()
                .status("error")
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .method("GET")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseData);
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_ERROR", 
                "An unexpected error occurred while processing your request"
        );
        
        ResponseData responseData = ResponseData.error(
                errorResponse,
                request.getDescription(false).replace("uri=", ""),
                "GET"
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseData);
    }
}
