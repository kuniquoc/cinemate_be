package com.pbl6.cinemate.movie.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChunkUploadInitResponse(
                String uploadId,
                String filename,
                Long totalSize,
                Integer totalChunks,
                Integer chunkSize,
                String status,
                Instant expiresAt,
                UUID movieId) {
}