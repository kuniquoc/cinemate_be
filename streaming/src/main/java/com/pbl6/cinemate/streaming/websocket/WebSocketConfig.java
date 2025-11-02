package com.pbl6.cinemate.streaming.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @NonNull
    private final StreamingWebSocketHandler streamingWebSocketHandler;
    @NonNull
    private final SignalingHandshakeInterceptor signalingHandshakeInterceptor;

    public WebSocketConfig(
            @NonNull StreamingWebSocketHandler streamingWebSocketHandler,
            @NonNull SignalingHandshakeInterceptor signalingHandshakeInterceptor) {
        this.streamingWebSocketHandler = streamingWebSocketHandler;
        this.signalingHandshakeInterceptor = signalingHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry
                .addHandler(streamingWebSocketHandler, "/ws/signaling")
                .addInterceptors(signalingHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

}
