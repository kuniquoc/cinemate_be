package com.pbl6.cinemate.payment_service.dto.response;

import com.pbl6.cinemate.payment_service.dto.CategoryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
    private List<CategoryDto> blockedCategories;
    private Integer watchTimeLimitMinutes;
    private Instant createdAt;
    private Instant updatedAt;
}
