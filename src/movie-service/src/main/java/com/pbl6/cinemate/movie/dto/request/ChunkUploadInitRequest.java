package com.pbl6.cinemate.movie.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChunkUploadInitRequest(
        @NotBlank(message = "Filename is required") String filename,

        @NotBlank(message = "MIME type is required") String mimeType,

        @NotNull(message = "Total size is required") @Min(value = 1, message = "Total size must be positive") Long totalSize,

        @NotNull(message = "Chunk size is required") @Min(value = 1024, message = "Chunk size must be at least 1KB") Integer chunkSize,

        @NotBlank(message = "Movie title is required") String movieTitle,

        String movieDescription) {
    public Integer getTotalChunks() {
        return (int) Math.ceil((double) totalSize / chunkSize);
    }
}