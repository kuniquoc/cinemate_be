package com.pbl6.cinemate.shared.streaming;

import java.util.Objects;

/**
 * Redis key patterns for streaming system.
 * Supports fMP4/DASH with multi-quality ABR.
 * 
 * Key structure for ABR:
 * - movie:{movieId}:quality:{qualityId}:segments - segments available for
 * specific quality
 * - movie:{movieId}:peers - peers streaming this movie (any quality)
 * - movie:{movieId}:quality:{qualityId}:segment:{segmentId}:owners - peers with
 * specific segment
 * - peer:{clientId}:lastSeen - last activity timestamp
 * - p2p:metrics:{clientId} - peer performance metrics
 * 
 * For ABR streaming:
 * - streamId = movieId (consistent across quality switches)
 * - qualityId is specified per segment request
 */
public final class StreamingRedisKeys {

    private static final String MOVIE_PREFIX = "movie:";
    private static final String QUALITY_INFIX = ":quality:";
    private static final String SEGMENT_INFIX = ":segment:";
    private static final String PEERS_SUFFIX = ":peers";
    private static final String SEGMENTS_SUFFIX = ":segments";
    private static final String OWNERS_SUFFIX = ":owners";
    private static final String PEER_PREFIX = "peer:";
    private static final String LAST_SEEN_SUFFIX = ":lastSeen";
    private static final String P2P_METRICS_PREFIX = "p2p:metrics:";

    private static final String MOVIE_ID_NAME = "movieId";
    private static final String SEGMENT_ID_NAME = "segmentId";
    private static final String CLIENT_ID_NAME = "clientId";
    private static final String QUALITY_ID_NAME = "qualityId";

    private StreamingRedisKeys() {
        // utility class
    }

    /**
     * Key for segments available for a specific movie quality.
     * Format: movie:{movieId}:quality:{qualityId}:segments
     * 
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (e.g., "720p", "1080p"), null for master
     *                  playlist
     * @return Redis key
     */
    public static String movieQualitySegmentsKey(String movieId, String qualityId) {
        String sanitizedMovieId = require(movieId, MOVIE_ID_NAME);
        if (qualityId == null || qualityId.isBlank()) {
            // Master playlist level
            return MOVIE_PREFIX + sanitizedMovieId + SEGMENTS_SUFFIX;
        }
        String sanitizedQualityId = require(qualityId, QUALITY_ID_NAME);
        return MOVIE_PREFIX + sanitizedMovieId + QUALITY_INFIX + sanitizedQualityId + SEGMENTS_SUFFIX;
    }

    /**
     * Pattern to match all quality segments for a movie.
     * Format: movie:{movieId}:quality:*:segments
     */
    public static String movieQualitySegmentsPattern(String movieId) {
        return MOVIE_PREFIX + require(movieId, MOVIE_ID_NAME) + QUALITY_INFIX + "*" + SEGMENTS_SUFFIX;
    }

    /**
     * Key for peers with a specific segment.
     * Format: movie:{movieId}:quality:{qualityId}:segment:{segmentId}:owners
     * 
     * @param movieId   the movie identifier
     * @param qualityId the quality variant
     * @param segmentId the segment identifier
     * @return Redis key
     */
    public static String segmentOwnersKey(String movieId, String qualityId, String segmentId) {
        String sanitizedMovieId = require(movieId, MOVIE_ID_NAME);
        String sanitizedSegmentId = require(segmentId, SEGMENT_ID_NAME);

        if (qualityId == null || qualityId.isBlank()) {
            // Master playlist or quality-agnostic
            return MOVIE_PREFIX + sanitizedMovieId + SEGMENT_INFIX + sanitizedSegmentId + OWNERS_SUFFIX;
        }

        String sanitizedQualityId = require(qualityId, QUALITY_ID_NAME);
        return MOVIE_PREFIX + sanitizedMovieId + QUALITY_INFIX + sanitizedQualityId
                + SEGMENT_INFIX + sanitizedSegmentId + OWNERS_SUFFIX;
    }

    /**
     * Key for all peers streaming a movie (across all qualities).
     * Format: movie:{movieId}:peers
     */
    public static String moviePeersKey(String movieId) {
        return MOVIE_PREFIX + require(movieId, MOVIE_ID_NAME) + PEERS_SUFFIX;
    }

    /**
     * Key for peer last seen timestamp.
     * Format: peer:{clientId}:lastSeen
     */
    public static String peerLastSeenKey(String clientId) {
        return PEER_PREFIX + require(clientId, CLIENT_ID_NAME) + LAST_SEEN_SUFFIX;
    }

    /**
     * Key for peer performance metrics.
     * Format: p2p:metrics:{clientId}
     */
    public static String peerMetricsKey(String clientId) {
        return P2P_METRICS_PREFIX + require(clientId, CLIENT_ID_NAME);
    }

    private static String require(String value, String name) {
        String sanitized = Objects.requireNonNull(value, name + " must not be null").trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return sanitized;
    }
}
