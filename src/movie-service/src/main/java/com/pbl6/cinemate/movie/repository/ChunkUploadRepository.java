package com.pbl6.cinemate.movie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbl6.cinemate.movie.entity.ChunkUpload;
import com.pbl6.cinemate.movie.enums.ChunkUploadStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChunkUploadRepository extends JpaRepository<ChunkUpload, UUID> {

    Optional<ChunkUpload> findByUploadId(String uploadId);

    List<ChunkUpload> findByStatus(ChunkUploadStatus status);

    @Query("SELECT c FROM ChunkUpload c WHERE c.expiresAt < :now AND c.status != :completedStatus")
    List<ChunkUpload> findExpiredUploads(@Param("now") Instant now,
                                         @Param("completedStatus") ChunkUploadStatus completedStatus);

    @Modifying
    @Query("DELETE FROM ChunkUpload c WHERE c.expiresAt < :now AND c.status = :expiredStatus")
    int deleteExpiredUploads(@Param("now") Instant now,
                             @Param("expiredStatus") ChunkUploadStatus expiredStatus);

    @Query("SELECT COUNT(c) FROM ChunkUpload c WHERE c.status = :status")
    long countByStatus(@Param("status") ChunkUploadStatus status);
}