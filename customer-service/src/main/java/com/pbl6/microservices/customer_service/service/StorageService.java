package com.pbl6.microservices.customer_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadImage(MultipartFile file);
}
