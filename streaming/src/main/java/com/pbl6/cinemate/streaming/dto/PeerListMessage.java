package com.pbl6.cinemate.streaming.dto;

import java.util.Set;

public record PeerListMessage(String type, String streamId, Set<String> peers) {

    public PeerListMessage(String streamId, Set<String> peers) {
        this("peer_list", streamId, peers);
    }
}
