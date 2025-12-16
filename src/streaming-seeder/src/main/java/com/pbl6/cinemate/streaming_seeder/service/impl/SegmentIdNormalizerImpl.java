package com.pbl6.cinemate.streaming_seeder.service.impl;

import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.service.SegmentIdNormalizer;

@Component
public class SegmentIdNormalizerImpl implements SegmentIdNormalizer {

    private static final String SEGMENT_PREFIX = "seg_";
    private static final String SEGMENT_ID_FORMAT = "%04d";
    private static final String M4S_EXTENSION = ".m4s";

    @Override
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

    @Override
    public String sanitize(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return null;
        }
        return segmentId.trim();
    }
}
