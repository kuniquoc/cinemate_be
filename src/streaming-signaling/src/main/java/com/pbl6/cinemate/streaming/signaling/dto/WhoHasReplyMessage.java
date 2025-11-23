package com.pbl6.cinemate.streaming.signaling.dto;

import java.util.List;

public record WhoHasReplyMessage(String type, String qualityId, String segmentId, List<PeerInfo> peers) {

    public WhoHasReplyMessage(String qualityId, String segmentId, List<PeerInfo> peers) {
        this("whoHasReply", qualityId, segmentId, peers);
    }
}
