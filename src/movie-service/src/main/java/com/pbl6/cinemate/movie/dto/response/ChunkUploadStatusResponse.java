package com.pbl6.cinemate.movie.dto.response;

import java.time.Instant;
import java.util.List;

public record ChunkUploadStatusResponse(
        String uploadId,
        String filename,
        Long totalSize,
        Integer totalChunks,
        Integer uploadedChunks,
        String status,
        Double progressPercentage,
        List<Integer> missingChunks,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        String movieTitle,
        String movieDescription) {
    public ChunkUploadStatusResponse(String uploadId, String filename, Long totalSize,
            Integer totalChunks, Integer uploadedChunks, String status,
            List<Integer> missingChunks, Instant createdAt,
            Instant updatedAt, Instant expiresAt,
            String movieTitle, String movieDescription) {
        this(uploadId, filename, totalSize, totalChunks, uploadedChunks, status,
                calculateProgress(uploadedChunks, totalChunks), missingChunks,
                createdAt, updatedAt, expiresAt, movieTitle, movieDescription);
    }

    private static Double calculateProgress(Integer uploadedChunks, Integer totalChunks) {
        if (totalChunks == null || totalChunks == 0)
            return 0.0;
        return Math.round((uploadedChunks.doubleValue() / totalChunks) * 10000.0) / 100.0;
    }
}