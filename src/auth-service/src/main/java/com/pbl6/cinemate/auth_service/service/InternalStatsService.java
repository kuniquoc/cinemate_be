package com.pbl6.cinemate.auth_service.service;

/**
 * Service for internal statistics
 */
public interface InternalStatsService {

    /**
     * Get total count of users
     * 
     * @return total user count
     */
    long getUsersCount();
}
