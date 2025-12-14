package com.pbl6.cinemate.shared.utils;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

/**
 * Utility class for generating UUIDs.
 * Uses UUID v7 (time-ordered) for better database indexing performance.
 */
public final class UuidGenerator {

    private UuidGenerator() {
        // Utility class - prevent instantiation
    }

    /**
     * Generates a new UUID v7 (time-ordered UUID as per RFC 9562).
     * UUID v7 combines a Unix timestamp with random bits, providing:
     * - Time-ordered values for better database index performance
     * - Sufficient randomness to avoid collisions
     * - Sortable by creation time
     *
     * @return a new UUID v7
     */
    public static UUID generateV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
