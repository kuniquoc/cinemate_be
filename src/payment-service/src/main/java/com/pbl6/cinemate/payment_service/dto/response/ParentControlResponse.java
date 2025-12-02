package com.pbl6.cinemate.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentControlResponse {
    private UUID id;
    private UUID parentId;
    private UUID kidId;
    private UUID subscriptionId;
    private List<String> blockedCategories;
    private Integer watchTimeLimitMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
