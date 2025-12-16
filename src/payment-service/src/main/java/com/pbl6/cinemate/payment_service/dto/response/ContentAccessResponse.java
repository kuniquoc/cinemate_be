package com.pbl6.cinemate.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAccessResponse {

    private Boolean allowed;

    private String reason;

    private Boolean isKid;

    private Integer remainingWatchTimeMinutes;

    private List<UUID> blockedCategoryIds;
}
