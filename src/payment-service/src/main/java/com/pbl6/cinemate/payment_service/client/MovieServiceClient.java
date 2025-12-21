package com.pbl6.cinemate.payment_service.client;

import com.pbl6.cinemate.shared.dto.general.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "movie-service", url = "${movie.service.url:http://localhost:8081}")
public interface MovieServiceClient {

    @GetMapping("/api/v1/categories")
    ResponseData getAllCategories();

    @GetMapping("/api/v1/categories/{id}")
    ResponseData getCategoryById(@PathVariable("id") UUID id);
}
