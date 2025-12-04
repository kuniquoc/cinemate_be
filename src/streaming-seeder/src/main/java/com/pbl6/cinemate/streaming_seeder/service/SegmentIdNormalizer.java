package com.pbl6.cinemate.streaming_seeder.service;

/**
 * Normalizes segment identifiers to a standard format.
 */
public interface SegmentIdNormalizer {

    /**
     * Normalizes segment IDs to standard format.
     *
     * @param segmentId the segment ID to normalize
     * @return the normalized segment ID
     */
    String normalize(String segmentId);

    /**
     * Sanitizes the segment ID (trims whitespace).
     *
     * @param segmentId the segment ID to sanitize
     * @return the sanitized segment ID, or null if invalid
     */
    String sanitize(String segmentId);
}
