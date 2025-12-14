package com.pbl6.cinemate.movie.service;

import org.springframework.web.multipart.MultipartFile;

import com.pbl6.cinemate.movie.dto.request.ChunkUploadInitRequest;
import com.pbl6.cinemate.movie.dto.request.ChunkUploadRequest;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadInitResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;

public interface ChunkUploadService {
    ChunkUploadInitResponse initiateUpload(ChunkUploadInitRequest request);

    ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber,
                                    MultipartFile chunkFile, ChunkUploadRequest request);

    ChunkUploadStatusResponse getUploadStatus(String uploadId);

    MovieUploadResponse completeUpload(String uploadId);

    void cleanupUploadFiles(String uploadId);

    void cancelUpload(String uploadId);

    void cleanupChunksAsync(String uploadId);
}
