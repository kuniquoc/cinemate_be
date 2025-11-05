package com.pbl6.cinemate.movie.controller;

import com.pbl6.cinemate.movie.dto.general.ResponseData;
import com.pbl6.cinemate.movie.dto.request.ChunkUploadInitRequest;
import com.pbl6.cinemate.movie.dto.request.ChunkUploadRequest;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadInitResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadResponse;
import com.pbl6.cinemate.movie.dto.response.ChunkUploadStatusResponse;
import com.pbl6.cinemate.movie.dto.response.MovieUploadResponse;
import com.pbl6.cinemate.movie.service.ChunkUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/movies/chunk-upload")
@RequiredArgsConstructor
@Tag(name = "Chunk Upload", description = "Chunked file upload for large video files")
public class ChunkUploadController {

        private final ChunkUploadService chunkUploadService;

        @Operation(summary = "Initiate chunk upload", description = "Create a new chunked upload session for a large video file")
        @PostMapping("/initiate")
        public ResponseEntity<ResponseData> initiateUpload(
                        @Valid @RequestBody ChunkUploadInitRequest request,
                        HttpServletRequest httpServletRequest) {

                ChunkUploadInitResponse response = chunkUploadService.initiateUpload(request);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Chunk upload session initiated successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Upload a chunk", description = "Upload a single chunk of the file")
        @PostMapping("/{uploadId}/chunks/{chunkNumber}")
        public ResponseEntity<ResponseData> uploadChunk(
                        @Parameter(description = "Upload session ID") @PathVariable String uploadId,
                        @Parameter(description = "Chunk number (0-based)") @PathVariable Integer chunkNumber,
                        @Parameter(description = "Chunk file data") @RequestPart("chunk") MultipartFile chunkFile,
                        @Parameter(description = "Chunk metadata") @RequestPart("data") @Valid ChunkUploadRequest request,
                        HttpServletRequest httpServletRequest) {

                ChunkUploadResponse response = chunkUploadService.uploadChunk(uploadId, chunkNumber, chunkFile,
                                request);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Chunk uploaded successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Get upload status", description = "Get the current status and progress of a chunk upload session")
        @GetMapping("/{uploadId}/status")
        public ResponseEntity<ResponseData> getUploadStatus(
                        @Parameter(description = "Upload session ID") @PathVariable String uploadId,
                        HttpServletRequest httpServletRequest) {

                ChunkUploadStatusResponse response = chunkUploadService.getUploadStatus(uploadId);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Upload status retrieved successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Complete upload", description = "Complete the chunk upload and merge all chunks into the final file")
        @PostMapping("/{uploadId}/complete")
        public ResponseEntity<ResponseData> completeUpload(
                        @Parameter(description = "Upload session ID") @PathVariable String uploadId,
                        HttpServletRequest httpServletRequest) {

                MovieUploadResponse response = chunkUploadService.completeUpload(uploadId);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Upload completed successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

        @Operation(summary = "Cancel upload", description = "Cancel an ongoing chunk upload session and clean up resources")
        @DeleteMapping("/{uploadId}")
        public ResponseEntity<ResponseData> cancelUpload(
                        @Parameter(description = "Upload session ID") @PathVariable String uploadId,
                        HttpServletRequest httpServletRequest) {

                chunkUploadService.cancelUpload(uploadId);

                return ResponseEntity.ok(ResponseData.success(
                                null,
                                "Upload cancelled successfully",
                                httpServletRequest.getRequestURI(),
                                httpServletRequest.getMethod()));
        }

}