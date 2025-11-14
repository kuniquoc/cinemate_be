package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.seeder.SegmentKey.SegmentType;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Represents a cached segment on the seeder's local disk.
 * Supports fMP4/DASH format with multi-quality ABR.
 *
 * @param movieId      the unique identifier of the movie (streamId for ABR)
 * @param qualityId    the quality variant (e.g., "720p", "1080p"), null for
 *                     master playlist
 * @param segmentId    the segment identifier (e.g., "init", "seg_0001",
 *                     "master")
 * @param path         the file system path to the cached segment
 * @param lastModified the last modification time
 * @param type         the type of segment (init, playlist, media)
 */
public record CachedSegment(
        String movieId,
        String qualityId,
        String segmentId,
        Path path,
        Instant lastModified,
        SegmentType type) {

    public CachedSegment {
        Objects.requireNonNull(movieId, "movieId must not be null");
        Objects.requireNonNull(segmentId, "segmentId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(lastModified, "lastModified must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Converts this cached segment to a SegmentKey.
     */
    public SegmentKey toSegmentKey() {
        return new SegmentKey(movieId, qualityId, segmentId);
    }

    /**
     * Checks if this segment is critical (init or playlist) and should not expire.
     */
    public boolean isCritical() {
        return type == SegmentType.INIT
                || type == SegmentType.MASTER_PLAYLIST
                || type == SegmentType.VARIANT_PLAYLIST;
    }
}
