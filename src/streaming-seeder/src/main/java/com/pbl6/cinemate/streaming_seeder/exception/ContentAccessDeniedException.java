package com.pbl6.cinemate.streaming_seeder.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Exception thrown when a user is denied access to streaming content
 */
@Getter
public class ContentAccessDeniedException extends RuntimeException {
    
    private final String reason;
    private final Boolean isKid;
    private final Integer remainingWatchTimeMinutes;
    private final List<String> blockedCategoryNames;

    public ContentAccessDeniedException(String reason) {
        super(reason);
        this.reason = reason;
        this.isKid = false;
        this.remainingWatchTimeMinutes = null;
        this.blockedCategoryNames = null;
    }

    public ContentAccessDeniedException(String reason, Boolean isKid, 
                                       Integer remainingWatchTimeMinutes, 
                                       List<String> blockedCategoryNames) {
        super(reason);
        this.reason = reason;
        this.isKid = isKid;
        this.remainingWatchTimeMinutes = remainingWatchTimeMinutes;
        this.blockedCategoryNames = blockedCategoryNames;
    }
}
