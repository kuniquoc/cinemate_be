package com.pbl6.cinemate.shared.streaming;

import java.util.Objects;

public final class StreamingRedisKeys {

    private static final String STREAM_PREFIX = "stream:";
    private static final String SEGMENT_SUFFIX = ":segment:";
    private static final String PEERS_SUFFIX = ":peers";
    private static final String SEGMENTS_SUFFIX = ":segments";
    private static final String PEER_PREFIX = "peer:";
    private static final String LAST_SEEN_SUFFIX = ":lastSeen";
    private static final String P2P_METRICS_PREFIX = "p2p:metrics:";
    private static final String STREAM_ID_NAME = "streamId";
    private static final String SEGMENT_ID_NAME = "segmentId";
    private static final String CLIENT_ID_NAME = "clientId";

    private StreamingRedisKeys() {
        // utility class
    }

    public static String segmentOwnersKey(String streamId, String segmentId) {
        return STREAM_PREFIX + require(streamId, STREAM_ID_NAME) + SEGMENT_SUFFIX + require(segmentId, SEGMENT_ID_NAME);
    }

    public static String streamPeersKey(String streamId) {
        return STREAM_PREFIX + require(streamId, STREAM_ID_NAME) + PEERS_SUFFIX;
    }

    public static String streamSegmentsDirectoryKey(String streamId) {
        return STREAM_PREFIX + require(streamId, STREAM_ID_NAME) + SEGMENTS_SUFFIX;
    }

    public static String peerLastSeenKey(String clientId) {
        return PEER_PREFIX + require(clientId, CLIENT_ID_NAME) + LAST_SEEN_SUFFIX;
    }

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
