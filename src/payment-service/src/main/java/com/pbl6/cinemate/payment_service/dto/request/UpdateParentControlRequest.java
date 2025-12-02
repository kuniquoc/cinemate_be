package com.pbl6.cinemate.payment_service.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class UpdateParentControlRequest {
    
    private List<String> blockedCategories; // List of category names to block from kid
    
    @Min(value = 0, message = "Watch time limit must be positive")
    private Integer watchTimeLimitMinutes;
}
