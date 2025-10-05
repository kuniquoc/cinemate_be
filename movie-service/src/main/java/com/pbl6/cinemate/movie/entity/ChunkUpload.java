package com.pbl6.cinemate.movie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import com.pbl6.cinemate.movie.enums.ChunkUploadStatus;

@Entity
@Table(name = "chunk_uploads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String uploadId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long totalSize;

    @Column(nullable = false)
    private Integer totalChunks;

    @Column(nullable = false)
    private Integer chunkSize;

    @Column(nullable = false)
    private Integer uploadedChunks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChunkUploadStatus status;

    @Column(columnDefinition = "text")
    private String uploadedChunksList; // JSON array of uploaded chunk numbers

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant expiresAt;

    // Movie info to create after upload completes
    private String movieTitle;
    private String movieDescription;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.expiresAt == null) {
            // Default expiry: 24 hours from creation
            this.expiresAt = this.createdAt.plusSeconds(24L * 60 * 60);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public ChunkUpload(String uploadId, String filename, String mimeType, Long totalSize,
            Integer totalChunks, Integer chunkSize, String movieTitle, String movieDescription) {
        this.uploadId = uploadId;
        this.filename = filename;
        this.mimeType = mimeType;
        this.totalSize = totalSize;
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
        this.uploadedChunks = 0;
        this.status = ChunkUploadStatus.INITIATED;
        this.uploadedChunksList = "[]";
        this.movieTitle = movieTitle;
        this.movieDescription = movieDescription;
    }
}