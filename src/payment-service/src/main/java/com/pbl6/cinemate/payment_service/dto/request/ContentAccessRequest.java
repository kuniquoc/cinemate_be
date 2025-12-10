package com.pbl6.cinemate.payment_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentAccessRequest {
    
    @NotEmpty(message = "Movie categories are required")
    private List<String> movieCategories;
    
    @NotNull(message = "Current watch time is required")
    @PositiveOrZero(message = "Watch time cannot be negative")
    private Integer currentWatchTimeMinutes;
}
