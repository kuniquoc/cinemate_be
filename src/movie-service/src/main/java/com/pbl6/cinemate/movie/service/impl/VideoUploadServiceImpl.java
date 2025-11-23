package com.pbl6.cinemate.movie.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.response.VideoUploadResponse;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.VideoUploadService;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.InternalServerException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadServiceImpl implements VideoUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mkv", "avi", "mov", "webm", "flv", "wmv");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB

    private final MinioStorageService minioStorageService;

    @Value("${minio.video-bucket:videos}")
    private String videoBucket;

    @Override
    public VideoUploadResponse upload(MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String objectPath = buildObjectPath(extension);

        Path tempFile = createTempFile(extension);
        try {
            File tempPhysicalFile = Objects.requireNonNull(tempFile.toFile(), "Temporary file conversion failed");
            file.transferTo(tempPhysicalFile);
            String url = minioStorageService.save(tempPhysicalFile, videoBucket, objectPath);
            return new VideoUploadResponse(url);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException("Failed to upload video: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("Unsupported video content type");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Video file size exceeds maximum allowed size of 5GB");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Video file must have a valid name");
        }

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == originalFilename.length() - 1) {
            throw new BadRequestException("Video file must have an extension");
        }

        String extension = originalFilename.substring(lastDot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "Unsupported video extension. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        return extension;
    }

    private String buildObjectPath(String extension) {
        String uniqueName = UUID.randomUUID().toString();
        return uniqueName + "." + extension;
    }

    private Path createTempFile(String extension) {
        try {
            return Files.createTempFile("video-upload-", "." + extension);
        } catch (IOException e) {
            throw new InternalServerException("Failed to create temporary file for video upload: " + e.getMessage());
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("Failed to delete temporary video file {}: {}", tempFile, e.getMessage());
        }
    }
}
