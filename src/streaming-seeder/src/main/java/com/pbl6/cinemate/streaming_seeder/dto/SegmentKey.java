package com.pbl6.cinemate.streaming_seeder.dto;

import java.util.Objects;

public record SegmentKey(String movieId, String qualityId, String segmentId) {

    public SegmentKey {
        Objects.requireNonNull(movieId, "movieId must not be null");
        Objects.requireNonNull(segmentId, "segmentId must not be null");
        // qualityId can be null for master playlist
    }

    /**
     * Determines the type of segment based on segmentId.
     */
    public SegmentType getType() {
        if (segmentId == null) {
            return SegmentType.MEDIA;
        }
        String lower = segmentId.toLowerCase();
        if (lower.equals("init") || lower.startsWith("init.")) {
            return SegmentType.INIT;
        }
        if (lower.equals("master") || lower.startsWith("master.")) {
            return SegmentType.MASTER_PLAYLIST;
        }
        if (lower.equals("playlist") || lower.startsWith("playlist.") || lower.endsWith(".m3u8")) {
            return SegmentType.VARIANT_PLAYLIST;
        }
        return SegmentType.MEDIA;
    }

    public enum SegmentType {
        /**
         * Init segment (init.mp4 or init.m4s) - critical for playback start
         */
        INIT,
        /**
         * Master playlist (master.m3u8) - for ABR
         */
        MASTER_PLAYLIST,
        /**
         * Variant playlist (playlist.m3u8 or {quality}.m3u8) - per quality
         */
        VARIANT_PLAYLIST,
        /**
         * Media segment (seg_0001.m4s) - actual video/audio data
         */
        MEDIA
    }
}
