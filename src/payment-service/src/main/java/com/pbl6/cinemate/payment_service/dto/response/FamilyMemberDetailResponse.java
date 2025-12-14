package com.pbl6.cinemate.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDetailResponse {
    private UUID id;
    private UUID userId;
    private String email;
    private Boolean isOwner;
    private Boolean isKid;
    private LocalDateTime joinedAt;
}
