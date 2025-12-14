package com.pbl6.cinemate.movie.entity;

import com.pbl6.cinemate.movie.enums.ChunkUploadStatus;
import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chunk_uploads")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUpload extends AbstractBaseEntity {

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

    private Instant expiresAt;

    // Reference to the movie being uploaded
    @Column(nullable = false)
    private UUID movieId;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.expiresAt == null) {
            // Default expiry: 24 hours from creation
            this.expiresAt = getCreatedAt().plusSeconds(24 * 3600L);
        }
    }
}