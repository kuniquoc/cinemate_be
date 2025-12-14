package com.pbl6.cinemate.payment_service.client;

import com.pbl6.cinemate.payment_service.dto.CategoryDto;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "movie-service", url = "${movie.service.url:http://localhost:8083}")
public interface MovieServiceClient {
    
    @GetMapping("/api/categories")
    ResponseData getAllCategories();
    
    @GetMapping("/api/categories/{id}")
    ResponseData getCategoryById(@PathVariable("id") UUID id);
}
