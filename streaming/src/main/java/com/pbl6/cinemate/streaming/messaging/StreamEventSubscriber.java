package com.pbl6.cinemate.streaming.messaging;

public interface StreamEventSubscriber {
    void ensureSubscribed(String streamId);
}
