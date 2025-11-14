package com.pbl6.cinemate.streaming.seeder.service;

import org.springframework.stereotype.Component;

/**
 * Normalizes segment identifiers to a standard format.
 * Since segmentId now includes the file extension (e.g., "seg_0005.m4s"),
 * this class simply validates and passes through the identifier.
 */
@Component
public class SegmentIdNormalizer {

    private static final String SEGMENT_PREFIX = "seg_";
    private static final String SEGMENT_ID_FORMAT = "%04d";
    private static final String M4S_EXTENSION = ".m4s";

    /**
     * Normalizes segment IDs to standard format.
     * Numeric IDs (e.g., "1", "42") are converted to seg_XXXX.m4s format (e.g.,
     * "seg_0001.m4s", "seg_0042.m4s").
     * Non-numeric IDs (e.g., "init.mp4", "playlist.m3u8", "master.m3u8") are kept
     * as-is.
     * IDs already in seg_XXXX.m4s format are kept as-is.
     */
    public String normalize(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return segmentId;
        }

        String trimmed = segmentId.trim();

        // If already in seg_XXXX.m4s format, keep it
        if (trimmed.startsWith(SEGMENT_PREFIX) && trimmed.endsWith(M4S_EXTENSION)) {
            return trimmed;
        }

        // If numeric, convert to seg_XXXX.m4s format
        try {
            int segmentNumber = Integer.parseInt(trimmed);
            return SEGMENT_PREFIX + String.format(SEGMENT_ID_FORMAT, segmentNumber) + M4S_EXTENSION;
        } catch (NumberFormatException e) {
            // Keep original for non-numeric IDs (init.mp4, playlist.m3u8, master.m3u8)
            return trimmed;
        }
    }

    /**
     * Validates that the segment ID is not null or blank.
     * No sanitization is performed since segmentId is now the complete filename.
     */
    public String sanitize(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return null;
        }
        return segmentId.trim();
    }
}
