package com.pbl6.cinemate.movie.dto.response;

public record ChunkUploadResponse(
        String uploadId,
        Integer chunkNumber,
        String status,
        String message,
        Integer uploadedChunks,
        Integer totalChunks,
        Double progressPercentage) {
    public ChunkUploadResponse(String uploadId, Integer chunkNumber, String status,
            String message, Integer uploadedChunks, Integer totalChunks) {
        this(uploadId, chunkNumber, status, message, uploadedChunks, totalChunks,
                calculateProgress(uploadedChunks, totalChunks));
    }

    private static Double calculateProgress(Integer uploadedChunks, Integer totalChunks) {
        if (totalChunks == null || totalChunks == 0)
            return 0.0;
        return Math.round((uploadedChunks.doubleValue() / totalChunks) * 10000.0) / 100.0;
    }
}