package com.pbl6.cinemate.movie.client.dto;

import java.util.List;
import java.util.Map;

/**
 * Response from customer-service internal API for favorite stats
 */
public record FavoriteStatsResponse(List<Map<String, Object>> data) {

    /**
     * Single favorite stat item
     */
    public record FavoriteStat(String date, String movieId, long count) {
    }
}