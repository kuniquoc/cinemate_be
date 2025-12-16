package com.pbl6.cinemate.movie.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        UUID movieId) {
    public ChunkUploadStatusResponse(String uploadId, String filename, Long totalSize,
                                     Integer totalChunks, Integer uploadedChunks, String status,
                                     List<Integer> missingChunks, Instant createdAt,
                                     Instant updatedAt, Instant expiresAt,
                                     UUID movieId) {
        this(uploadId, filename, totalSize, totalChunks, uploadedChunks, status,
                calculateProgress(uploadedChunks, totalChunks), missingChunks,
                createdAt, updatedAt, expiresAt, movieId);
    }

    private static Double calculateProgress(Integer uploadedChunks, Integer totalChunks) {
        if (totalChunks == null || totalChunks == 0)
            return 0.0;
        return Math.round((uploadedChunks.doubleValue() / totalChunks) * 10000.0) / 100.0;
    }
}