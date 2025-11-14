package com.pbl6.cinemate.streaming.signaling.dto;

import java.util.List;

public record WhoHasReplyMessage(String type, String segmentId, List<PeerInfo> peers) {

    public WhoHasReplyMessage(String segmentId, List<PeerInfo> peers) {
        this("whoHasReply", segmentId, peers);
    }
}
