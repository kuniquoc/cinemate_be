package com.pbl6.cinemate.streaming_signaling.websocket.interceptor;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SignalingHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_MOVIE_ID = "movieId";
    private static final Logger log = LoggerFactory.getLogger(SignalingHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        // Accept clientId
        Optional.ofNullable(queryParams.getFirst(ATTR_CLIENT_ID))
                .ifPresent(clientId -> attributes.put(ATTR_CLIENT_ID, clientId));

        // Accept movieId
        Optional.ofNullable(queryParams.getFirst(ATTR_MOVIE_ID))
                .ifPresent(movieId -> attributes.put(ATTR_MOVIE_ID, movieId));

        if (!attributes.containsKey(ATTR_CLIENT_ID) || !attributes.containsKey(ATTR_MOVIE_ID)) {
            log.warn("Handshake rejected: missing clientId or movieId. uri={}", request.getURI());
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception) {
        // no-op
    }
}
