package com.pbl6.cinemate.streaming.signaling.messaging;

public interface StreamEventSubscriber {
    void ensureSubscribed(String streamId);
}
