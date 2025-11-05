package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChunkUploadRequest(
        @NotBlank(message = "Upload ID is required") String uploadId,

        @NotNull(message = "Chunk number is required") @Min(value = 0, message = "Chunk number must be non-negative") Integer chunkNumber,

        @NotNull(message = "Chunk size is required") @Min(value = 1, message = "Chunk size must be positive") Integer chunkSize,

        String checksum // Optional MD5 checksum for integrity verification
) {
}