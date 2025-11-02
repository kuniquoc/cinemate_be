package com.pbl6.cinemate.streaming.dto;

import java.util.List;

public record WhoHasReplyMessage(String type, String segmentId, List<PeerInfo> peers) {

    public WhoHasReplyMessage(String segmentId, List<PeerInfo> peers) {
        this("WHO_HAS_REPLY", segmentId, peers);
    }
}
