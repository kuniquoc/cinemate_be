package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.repository.FavoriteRepository;
import com.pbl6.microservices.customer_service.service.InternalStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternalStatsServiceImpl implements InternalStatsService {

    private final FavoriteRepository favoriteRepository;

    @Override
    public List<Map<String, Object>> getFavoriteStats(Instant startDate, Instant endDate) {

        List<Object[]> results = favoriteRepository.countFavoritesByDateAndMovie(startDate, endDate);

        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString());
            item.put("movieId", row[1].toString());
            item.put("count", ((Number) row[2]).longValue());
            response.add(item);
        }

        return response;
    }
}