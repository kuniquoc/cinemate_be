package com.pbl6.cinemate.payment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberResponse {
    private UUID id;
    private UUID userId;
    private Boolean isOwner;
    private Instant joinedAt;
}
