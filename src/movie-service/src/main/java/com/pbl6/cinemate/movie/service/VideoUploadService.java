package com.pbl6.cinemate.movie.service;

import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.response.VideoUploadResponse;

public interface VideoUploadService {
    VideoUploadResponse upload(MultipartFile file);
}
