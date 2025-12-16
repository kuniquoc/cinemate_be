package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.response.ImageUploadResponse;
import com.pbl6.cinemate.movie.service.ImageUploadService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload-image")
@RequiredArgsConstructor
@Tag(name = "Image Upload", description = "Upload poster or thumbnail images to MinIO storage")
@PreAuthorize("hasRole('ADMIN')")
public class UploadImageController {

    private final ImageUploadService imageUploadService;

    @Operation(summary = "Upload image", description = "Upload an image file and receive its publicly accessible URL (Admin only)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData> uploadImage(
            @Parameter(description = "Image file to upload") @RequestPart("file") MultipartFile file,
            HttpServletRequest httpServletRequest) {

        ImageUploadResponse response = imageUploadService.upload(file);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Image uploaded successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}