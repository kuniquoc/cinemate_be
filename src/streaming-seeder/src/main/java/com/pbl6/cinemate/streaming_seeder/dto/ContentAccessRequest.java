package com.pbl6.cinemate.streaming_seeder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentAccessRequest {

    private UUID userId;
    
    private List<UUID> movieCategoryIds;

    private Integer currentWatchTimeMinutes;
}
