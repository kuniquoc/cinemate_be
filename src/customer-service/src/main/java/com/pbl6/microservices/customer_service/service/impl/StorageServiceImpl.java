package com.pbl6.microservices.customer_service.service.impl;

import com.pbl6.microservices.customer_service.config.AppProperties;
import com.pbl6.microservices.customer_service.constants.ErrorMessage;
import com.pbl6.microservices.customer_service.exception.BadRequestException;
import com.pbl6.microservices.customer_service.exception.InternalServerException;
import com.pbl6.microservices.customer_service.service.StorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final MinioClient minioClient;
    private final AppProperties appProperties;

    @Override
    public String uploadImage(MultipartFile file) {
        validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID() + fileExtension;

            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(appProperties.getMinio().getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            inputStream.close();

            // Return the URL to access the file
            // TO DO: using deployed url
            String fileUrl = String.format("%s/%s/%s",
                    "http://localhost:9000",
                    appProperties.getMinio().getBucketName(),
                    fileName
            );

            log.info("File uploaded successfully: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new InternalServerException(ErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Only images are allowed (JPEG, PNG, GIF, WEBP)");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}
