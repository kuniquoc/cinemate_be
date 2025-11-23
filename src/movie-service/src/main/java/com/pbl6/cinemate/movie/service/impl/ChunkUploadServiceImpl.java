package com.pbl6.cinemate.movie.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.movie.dto.request.ChunkUploadInitRequest;
import com.pbl6.cinemate.movie.dto.request.ChunkUploadRequest;

import com.pbl6.cinemate.movie.dto.response.ChunkUploadInitResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;
import com.pbl6.cinemate.movie.entity.ChunkUpload;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.ChunkUploadStatus;
import com.pbl6.cinemate.movie.repository.ChunkUploadRepository;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.ChunkUploadService;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.InternalServerException;
import com.pbl6.cinemate.shared.exception.NotFoundException;
import com.pbl6.cinemate.movie.event.MovieCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements ChunkUploadService {
    private static final String UPLOAD_SESSION_NOT_FOUND = "Upload session not found: ";
    private static final String MOVIE_NOT_FOUND = "Movie not found: ";

    private final ChunkUploadRepository chunkUploadRepository;
    private final MovieRepository movieRepository;
    private final MinioStorageService minioStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${ffmpeg.tmp-dir:/tmp/movies}")
    private String tmpBaseDir;

    @Value("${chunk-upload.max-file-size:5368709120}") // 5GB default
    private Long maxFileSize;

    @Value("${chunk-upload.min-chunk-size:1048576}") // 1MB default
    private Integer minChunkSize;

    @Value("${chunk-upload.max-chunk-size:104857600}") // 100MB default
    private Integer maxChunkSize;

    @Value("${minio.movie-bucket:}")
    private String movieBucket;

    @Transactional
    public ChunkUploadInitResponse initiateUpload(ChunkUploadInitRequest request) {
        validateUploadRequest(request);

        // Verify movie exists
        movieRepository.findById(request.movieId())
                .orElseThrow(() -> new NotFoundException(MOVIE_NOT_FOUND + request.movieId()));

        String uploadId = generateUploadId();
        ChunkUpload chunkUpload = new ChunkUpload(
                uploadId,
                request.filename(),
                request.mimeType(),
                request.totalSize(),
                request.getTotalChunks(),
                request.chunkSize(),
                request.movieId());

        chunkUpload = chunkUploadRepository.save(chunkUpload);
        createUploadDirectory(uploadId);

        log.info("Initiated chunk upload with ID: {} for file: {} and movie: {}",
                uploadId, request.filename(), request.movieId());

        return new ChunkUploadInitResponse(
                chunkUpload.getUploadId(),
                chunkUpload.getFilename(),
                chunkUpload.getTotalSize(),
                chunkUpload.getTotalChunks(),
                chunkUpload.getChunkSize(),
                chunkUpload.getStatus().name(),
                chunkUpload.getExpiresAt(),
                chunkUpload.getMovieId());
    }

    @Transactional
    public ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber,
            MultipartFile chunkFile, ChunkUploadRequest request) {
        ChunkUpload chunkUpload = chunkUploadRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_SESSION_NOT_FOUND + uploadId));

        validateChunkUpload(chunkUpload, chunkNumber, chunkFile);

        try {
            // Verify checksum if provided
            if (request.checksum() != null && !request.checksum().isEmpty()) {
                String actualChecksum = calculateMD5(chunkFile);
                if (!actualChecksum.equals(request.checksum())) {
                    throw new BadRequestException("Chunk checksum mismatch");
                }
            }

            // Save chunk directly to MinIO
            minioStorageService.saveChunk(chunkFile.getInputStream(), chunkFile.getSize(), movieBucket, uploadId,
                    chunkNumber);

            // Update chunk upload record
            updateChunkUploadProgress(chunkUpload, chunkNumber);

            log.debug("Successfully uploaded chunk {} for upload {} to MinIO", chunkNumber, uploadId);

            return new ChunkUploadResponse(
                    uploadId,
                    chunkNumber,
                    chunkUpload.getStatus().name(),
                    "Chunk uploaded successfully",
                    chunkUpload.getUploadedChunks(),
                    chunkUpload.getTotalChunks());

        } catch (Exception e) {
            log.error("Failed to upload chunk {} for upload {}: {}", chunkNumber, uploadId, e.getMessage());
            throw new InternalServerException("Failed to upload chunk: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ChunkUploadStatusResponse getUploadStatus(String uploadId) {
        ChunkUpload chunkUpload = chunkUploadRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_SESSION_NOT_FOUND + uploadId));

        List<Integer> missingChunks = calculateMissingChunks(chunkUpload);

        return new ChunkUploadStatusResponse(
                chunkUpload.getUploadId(),
                chunkUpload.getFilename(),
                chunkUpload.getTotalSize(),
                chunkUpload.getTotalChunks(),
                chunkUpload.getUploadedChunks(),
                chunkUpload.getStatus().name(),
                missingChunks,
                chunkUpload.getCreatedAt(),
                chunkUpload.getUpdatedAt(),
                chunkUpload.getExpiresAt(),
                chunkUpload.getMovieId());
    }

    @Transactional
    public MovieUploadResponse completeUpload(String uploadId) {
        ChunkUpload chunkUpload = chunkUploadRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_SESSION_NOT_FOUND + uploadId));

        if (chunkUpload.getStatus() != ChunkUploadStatus.IN_PROGRESS) {
            throw new BadRequestException("Upload is not in progress");
        }

        List<Integer> missingChunks = calculateMissingChunks(chunkUpload);
        if (!missingChunks.isEmpty()) {
            throw new BadRequestException("Upload incomplete. Missing chunks: " + missingChunks);
        }

        try {
            chunkUpload.setStatus(ChunkUploadStatus.MERGING);
            chunkUploadRepository.save(chunkUpload);

            // Get existing movie record
            Movie movie = movieRepository.findById(chunkUpload.getMovieId())
                    .orElseThrow(() -> new NotFoundException(MOVIE_NOT_FOUND + chunkUpload.getMovieId()));

            // Compose chunks directly in MinIO (server-side)
            String finalObjectPath = String.join("/", "original", movie.getId().toString(),
                    chunkUpload.getFilename());
            minioStorageService.composeChunks(movieBucket, uploadId, finalObjectPath, chunkUpload.getTotalChunks());

            // Create temporary file for transcoding
            Path tempFile = createTempFileFromMinIO(finalObjectPath, chunkUpload.getFilename());

            // Publish event to start transcoding after transaction commits
            eventPublisher.publishEvent(new MovieCreatedEvent(this, movie.getId(), tempFile));

            // Mark upload as completed
            chunkUpload.setStatus(ChunkUploadStatus.COMPLETED);
            chunkUploadRepository.save(chunkUpload);

            // Clean up chunks and temporary files asynchronously
            // Note: In production, this should be done via a separate scheduled task
            cleanupUploadFilesSync(uploadId);

            log.info("Successfully completed chunk upload {} and processed movie {}",
                    uploadId, movie.getId());

            return new MovieUploadResponse(movie.getId(), movie.getStatus().name());

        } catch (Exception e) {
            log.error("Failed to complete upload {}: {}", uploadId, e.getMessage());
            chunkUpload.setStatus(ChunkUploadStatus.FAILED);
            chunkUploadRepository.save(chunkUpload);
            throw new InternalServerException("Failed to complete upload: " + e.getMessage());
        }
    }

    private void validateUploadRequest(ChunkUploadInitRequest request) {
        if (request.totalSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed: " + maxFileSize + " bytes");
        }

        if (request.chunkSize() < minChunkSize || request.chunkSize() > maxChunkSize) {
            throw new BadRequestException("Chunk size must be between " + minChunkSize +
                    " and " + maxChunkSize + " bytes");
        }

        if (!isVideoFile(request.mimeType())) {
            throw new BadRequestException("Only video files are allowed");
        }
    }

    private void validateChunkUpload(ChunkUpload chunkUpload, Integer chunkNumber,
            MultipartFile chunkFile) {
        if (chunkUpload.getStatus() == ChunkUploadStatus.COMPLETED) {
            throw new BadRequestException("Upload already completed");
        }

        if (chunkUpload.getStatus() == ChunkUploadStatus.FAILED) {
            throw new BadRequestException("Upload failed, cannot accept more chunks");
        }

        if (chunkUpload.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Upload session expired");
        }

        if (chunkNumber >= chunkUpload.getTotalChunks()) {
            throw new BadRequestException("Invalid chunk number");
        }

        if (chunkFile.isEmpty()) {
            throw new BadRequestException("Chunk file is empty");
        }

        // Check if chunk already uploaded in MinIO
        if (minioStorageService.chunkExists(movieBucket, chunkUpload.getUploadId(), chunkNumber)) {
            throw new BadRequestException("Chunk already uploaded: " + chunkNumber);
        }
    }

    private String generateUploadId() {
        return UUID.randomUUID().toString();
    }

    private void createUploadDirectory(String uploadId) {
        try {
            Path uploadDir = Paths.get(tmpBaseDir, "chunks", uploadId);
            Files.createDirectories(uploadDir);
        } catch (Exception e) {
            throw new InternalServerException("Failed to create upload directory: " + e.getMessage());
        }
    }

    private void updateChunkUploadProgress(ChunkUpload chunkUpload, Integer chunkNumber) {
        try {
            List<Integer> uploadedChunks = parseUploadedChunksList(chunkUpload.getUploadedChunksList());
            uploadedChunks.add(chunkNumber);

            chunkUpload.setUploadedChunksList(objectMapper.writeValueAsString(uploadedChunks));
            chunkUpload.setUploadedChunks(uploadedChunks.size());

            if (chunkUpload.getStatus() == ChunkUploadStatus.INITIATED) {
                chunkUpload.setStatus(ChunkUploadStatus.IN_PROGRESS);
            }

            chunkUploadRepository.save(chunkUpload);
        } catch (Exception e) {
            throw new InternalServerException("Failed to update upload progress: " + e.getMessage());
        }
    }

    private List<Integer> calculateMissingChunks(ChunkUpload chunkUpload) {
        // Get existing chunks from MinIO for more accurate status
        List<Integer> existingChunks = minioStorageService.getExistingChunks(movieBucket, chunkUpload.getUploadId());
        List<Integer> missingChunks = new ArrayList<>();

        for (int i = 0; i < chunkUpload.getTotalChunks(); i++) {
            if (!existingChunks.contains(i)) {
                missingChunks.add(i);
            }
        }

        return missingChunks;
    }

    @Async
    public void cleanupUploadFiles(String uploadId) {
        try {
            // Clean up any temporary files created for transcoding
            Files.walk(Paths.get(tmpBaseDir))
                    .filter(path -> path.getFileName().toString().contains(uploadId))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete file: {}", path, e);
                        }
                    });

            log.debug("Cleaned up temporary files for upload: {}", uploadId);
        } catch (Exception e) {
            log.warn("Failed to cleanup upload files for {}: {}", uploadId, e.getMessage());
        }
    }

    private List<Integer> parseUploadedChunksList(String uploadedChunksList) {
        try {
            if (uploadedChunksList == null || uploadedChunksList.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(uploadedChunksList, new TypeReference<List<Integer>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse uploaded chunks list: {}", uploadedChunksList, e);
            return new ArrayList<>();
        }
    }

    private String calculateMD5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = file.getInputStream();
                    DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Reading to calculate digest
                }
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new InternalServerException("Failed to calculate MD5: " + e.getMessage());
        }
    }

    private Path createTempFileFromMinIO(String objectPath, String filename) {
        try {
            Path tempFile = Paths.get(tmpBaseDir, "temp_" + System.currentTimeMillis() + "_" + filename);

            // Download from MinIO to temporary file
            try (InputStream inputStream = minioStorageService.getObject(movieBucket, objectPath);
                    FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {
                inputStream.transferTo(outputStream);
            }

            return tempFile;
        } catch (Exception e) {
            throw new InternalServerException("Failed to create temp file from MinIO: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelUpload(String uploadId) {
        ChunkUpload chunkUpload = chunkUploadRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new NotFoundException(UPLOAD_SESSION_NOT_FOUND + uploadId));

        if (chunkUpload.getStatus() == ChunkUploadStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel completed upload");
        }

        try {
            // Mark as failed
            chunkUpload.setStatus(ChunkUploadStatus.FAILED);
            chunkUploadRepository.save(chunkUpload);

            // Clean up chunks from MinIO
            minioStorageService.cleanupChunks(movieBucket, uploadId);

            // Clean up local temporary files
            cleanupUploadFilesSync(uploadId);

            log.info("Successfully cancelled upload session: {}", uploadId);
        } catch (Exception e) {
            log.error("Failed to cancel upload {}: {}", uploadId, e.getMessage());
            throw new InternalServerException("Failed to cancel upload: " + e.getMessage());
        }
    }

    @Async
    public void cleanupChunksAsync(String uploadId) {
        try {
            // Clean up chunks from MinIO
            minioStorageService.cleanupChunks(movieBucket, uploadId);

            // Clean up any local temporary files
            cleanupUploadFilesSync(uploadId);

            log.debug("Cleaned up chunks and temporary files for upload: {}", uploadId);
        } catch (Exception e) {
            log.warn("Failed to cleanup chunks for {}: {}", uploadId, e.getMessage());
        }
    }

    private void cleanupUploadFilesSync(String uploadId) {
        try {
            // Clean up any temporary files created for transcoding
            Files.walk(Paths.get(tmpBaseDir))
                    .filter(path -> path.getFileName().toString().contains(uploadId))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete file: {}", path, e);
                        }
                    });

            log.debug("Cleaned up temporary files for upload: {}", uploadId);
        } catch (Exception e) {
            log.warn("Failed to cleanup upload files for {}: {}", uploadId, e.getMessage());
        }
    }

    private boolean isVideoFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }
}