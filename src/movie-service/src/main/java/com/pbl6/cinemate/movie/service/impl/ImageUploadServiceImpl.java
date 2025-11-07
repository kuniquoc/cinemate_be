package com.pbl6.cinemate.movie.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.response.ImageUploadResponse;
import com.pbl6.cinemate.movie.exception.BadRequestException;
import com.pbl6.cinemate.movie.exception.InternalServerException;
import com.pbl6.cinemate.movie.service.ImageUploadService;
import com.pbl6.cinemate.movie.service.MinioStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final MinioStorageService minioStorageService;

    @Value("${storage.image-prefix:images}")
    private String imagePrefix;

    @Override
    public ImageUploadResponse upload(MultipartFile file) {
        validateFile(file);

        String extension = resolveExtension(file.getOriginalFilename());
        String objectPath = buildObjectPath(extension);

        Path tempFile = createTempFile(extension);
        try {
            File tempPhysicalFile = Objects.requireNonNull(tempFile.toFile(), "Temporary file conversion failed");
            file.transferTo(tempPhysicalFile);
            String url = minioStorageService.save(tempPhysicalFile, objectPath);
            return new ImageUploadResponse(url);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException("Failed to upload image: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Unsupported image content type");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Image file must have a valid name");
        }

        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == originalFilename.length() - 1) {
            throw new BadRequestException("Image file must have an extension");
        }

        String extension = originalFilename.substring(lastDot + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported image extension");
        }

        return extension;
    }

    private String buildObjectPath(String extension) {
        String dateSegment = LocalDate.now().format(DATE_FOLDER_FORMAT);
        String uniqueName = UUID.randomUUID().toString();
        String normalizedPrefix = normalizePrefix(imagePrefix);
        return normalizedPrefix + "/" + dateSegment + "/" + uniqueName + "." + extension;
    }

    private String normalizePrefix(String prefix) {
        String sanitized = (prefix == null || prefix.isBlank()) ? "images" : prefix;
        sanitized = sanitized.replaceAll("^/+", "");
        sanitized = sanitized.replaceAll("/+$", "");
        return sanitized.isEmpty() ? "images" : sanitized;
    }

    private Path createTempFile(String extension) {
        try {
            return Files.createTempFile("image-upload-", "." + extension);
        } catch (IOException e) {
            throw new InternalServerException("Failed to create temporary file for image upload: " + e.getMessage());
        }
    }

    private void deleteTempFile(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("Failed to delete temporary image file {}: {}", tempFile, e.getMessage());
        }
    }
}
