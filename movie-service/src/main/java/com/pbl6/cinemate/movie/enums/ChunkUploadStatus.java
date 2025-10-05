package com.pbl6.cinemate.movie.enums;

public enum ChunkUploadStatus {
    INITIATED, // Upload session created
    IN_PROGRESS, // Chunks being uploaded
    COMPLETED, // All chunks uploaded
    MERGING, // Merging chunks into final file
    FAILED, // Upload failed
    EXPIRED // Upload session expired
}