package com.pbl6.cinemate.movie.scheduler;

import com.pbl6.cinemate.movie.entity.ChunkUpload;
import com.pbl6.cinemate.movie.enums.ChunkUploadStatus;
import com.pbl6.cinemate.movie.repository.ChunkUploadRepository;
import com.pbl6.cinemate.movie.service.ChunkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkUploadCleanupScheduler {

    private final ChunkUploadRepository chunkUploadRepository;
    private final ChunkUploadService chunkUploadService;

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredUploads() {
        log.debug("Starting cleanup of expired chunk uploads");

        Instant now = Instant.now();

        // Find expired uploads that are not completed
        List<ChunkUpload> expiredUploads = chunkUploadRepository.findExpiredUploads(now, ChunkUploadStatus.COMPLETED);

        if (expiredUploads.isEmpty()) {
            log.debug("No expired uploads found");
            return;
        }

        log.info("Found {} expired upload sessions to cleanup", expiredUploads.size());

        for (ChunkUpload upload : expiredUploads) {
            try {
                // Update status to expired
                upload.setStatus(ChunkUploadStatus.EXPIRED);
                chunkUploadRepository.save(upload);

                // Clean up chunks and files asynchronously
                chunkUploadService.cleanupChunksAsync(upload.getUploadId());

                log.debug("Marked upload {} as expired and cleaned up files", upload.getUploadId());
            } catch (Exception e) {
                log.error("Failed to cleanup expired upload {}: {}", upload.getUploadId(), e.getMessage());
            }
        }

        log.info("Completed cleanup of {} expired upload sessions", expiredUploads.size());
    }

    @Scheduled(fixedRate = 86400000) // Run daily
    @Transactional
    public void deleteOldCompletedUploads() {
        log.debug("Starting deletion of old completed chunk uploads");

        // Delete completed uploads older than 7 days
        Instant cutoff = Instant.now().minusSeconds(7 * 24 * 3600L);

        try {
            List<ChunkUpload> oldUploads = chunkUploadRepository.findExpiredUploads(cutoff, null);
            List<ChunkUpload> completedOldUploads = oldUploads.stream()
                    .filter(upload -> upload.getStatus() == ChunkUploadStatus.COMPLETED ||
                            upload.getStatus() == ChunkUploadStatus.EXPIRED ||
                            upload.getStatus() == ChunkUploadStatus.FAILED)
                    .toList();

            if (!completedOldUploads.isEmpty()) {
                chunkUploadRepository.deleteAll(completedOldUploads);
                log.info("Deleted {} old completed upload records", completedOldUploads.size());
            }
        } catch (Exception e) {
            log.error("Failed to delete old completed uploads: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    @Transactional(readOnly = true)
    public void logUploadStatistics() {
        try {
            long initiated = chunkUploadRepository.countByStatus(ChunkUploadStatus.INITIATED);
            long inProgress = chunkUploadRepository.countByStatus(ChunkUploadStatus.IN_PROGRESS);
            long completed = chunkUploadRepository.countByStatus(ChunkUploadStatus.COMPLETED);
            long failed = chunkUploadRepository.countByStatus(ChunkUploadStatus.FAILED);
            long expired = chunkUploadRepository.countByStatus(ChunkUploadStatus.EXPIRED);

            log.info("Upload statistics - Initiated: {}, In Progress: {}, Completed: {}, Failed: {}, Expired: {}",
                    initiated, inProgress, completed, failed, expired);
        } catch (Exception e) {
            log.error("Failed to log upload statistics: {}", e.getMessage());
        }
    }
}