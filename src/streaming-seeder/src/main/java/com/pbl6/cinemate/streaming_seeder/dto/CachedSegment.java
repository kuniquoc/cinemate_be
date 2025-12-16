package com.pbl6.cinemate.streaming_seeder.dto;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record CachedSegment(
        String movieId,
        String qualityId,
        String segmentId,
        Path path,
        Instant lastModified,
        SegmentKey.SegmentType type) {

    public CachedSegment {
        Objects.requireNonNull(movieId, "movieId must not be null");
        Objects.requireNonNull(segmentId, "segmentId must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(lastModified, "lastModified must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    public SegmentKey toSegmentKey() {
        return new SegmentKey(movieId, qualityId, segmentId);
    }

    /**
     * Checks if this segment is critical (init or playlist) and should not expire.
     */
    public boolean isCritical() {
        return type == SegmentKey.SegmentType.INIT
                || type == SegmentKey.SegmentType.MASTER_PLAYLIST
                || type == SegmentKey.SegmentType.VARIANT_PLAYLIST;
    }
}
