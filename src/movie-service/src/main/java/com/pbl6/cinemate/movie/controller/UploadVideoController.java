package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.dto.response.VideoUploadResponse;
import com.pbl6.cinemate.movie.service.VideoUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload-video")
@RequiredArgsConstructor
@Tag(name = "Video Upload", description = "Upload video files to MinIO storage")
public class UploadVideoController {

    private final VideoUploadService videoUploadService;

    @Operation(summary = "Upload video", description = "Upload a video file and receive its publicly accessible URL")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseData> uploadVideo(
            @Parameter(description = "Video file to upload") @RequestPart("file") MultipartFile file,
            HttpServletRequest httpServletRequest) {

        VideoUploadResponse response = videoUploadService.upload(file);

        return ResponseEntity.ok(ResponseData.success(
                response,
                "Video uploaded successfully",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getMethod()));
    }
}
