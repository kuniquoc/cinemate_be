package com.pbl6.cinemate.streaming_signaling.util.websocket;

import org.springframework.web.socket.WebSocketSession;

public final class Attrs {
    private Attrs() {
    }

    public static String get(WebSocketSession s, String key) {
        Object v = s.getAttributes().get(key);
        if (v instanceof String str && !str.trim().isEmpty()) {
            return str.trim();
        }
        throw new IllegalStateException("Missing attribute " + key);
    }
}
