package com.pbl6.cinemate.streaming_seeder.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Movie Service to get movie category IDs
 */
@FeignClient(name = "movie-service", url = "${movie.service.url:http://movie-service:8080}")
public interface MovieServiceClient {

    @GetMapping("/api/internal/movies/{movieId}/category-ids")
    List<UUID> getMovieCategoryIds(@PathVariable("movieId") UUID movieId);

    @GetMapping("/api/internal/categories/names")
    Map<String, String> getCategoryNames(@RequestParam("ids") List<UUID> categoryIds);
}
