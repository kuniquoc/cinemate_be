package com.pbl6.cinemate.movie.dto.response;

import java.time.Instant;

public record ChunkUploadInitResponse(
        String uploadId,
        String filename,
        Long totalSize,
        Integer totalChunks,
        Integer chunkSize,
        String status,
        Instant expiresAt,
        String movieTitle,
        String movieDescription) {
}