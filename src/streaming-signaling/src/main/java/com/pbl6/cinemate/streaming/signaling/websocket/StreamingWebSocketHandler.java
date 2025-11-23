package com.pbl6.cinemate.streaming.signaling.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.streaming.signaling.dto.ErrorMessage;
import com.pbl6.cinemate.streaming.signaling.dto.PeerListMessage;
import com.pbl6.cinemate.streaming.signaling.dto.ReportSegmentAckMessage;
import com.pbl6.cinemate.streaming.signaling.dto.WhoHasReplyMessage;
import com.pbl6.cinemate.streaming.signaling.service.SignalingService;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class StreamingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(StreamingWebSocketHandler.class);
    private final SignalingService signalingService;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public StreamingWebSocketHandler(SignalingService signalingService, ObjectMapper objectMapper) {
        this.signalingService = signalingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
        PeerListMessage peerListMessage = signalingService.registerClient(clientId, movieId);
        sessions.put(clientId, session);
        send(session, peerListMessage);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        JsonNode payload = objectMapper.readTree(message.getPayload());
        String typeRaw = payload.path("type").textValue();
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        if (typeRaw == null) {
            send(session, new ErrorMessage("Missing message type"));
            return;
        }
        String type = typeRaw.trim();
        if (type.isEmpty()) {
            send(session, new ErrorMessage("Missing message type"));
            return;
        }

        switch (type) {
            case "whoHas" -> handleWhoHas(session, payload);
            case "reportSegment" -> handleReportSegment(session, payload);
            case "removeSegment" -> handleRemoveSegment(session, payload);
            case "rtcOffer", "rtcAnswer", "iceCandidate" -> handleRtcRelay(session, payload);
            default -> send(session, new ErrorMessage("Unsupported message type: " + type));
        }
        log.debug("Handled {} message from client {}", type, clientId);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
        signalingService.handleDisconnect(clientId, movieId);
        sessions.remove(clientId);
    }

    private void handleWhoHas(WebSocketSession session, JsonNode payload) throws IOException {
        String movieIdRaw = payload.path("movieId").textValue();
        String qualityIdRaw = payload.path("qualityId").textValue();
        String segmentIdRaw = payload.path("segmentId").textValue();
        if (movieIdRaw == null || qualityIdRaw == null || segmentIdRaw == null) {
            send(session, new ErrorMessage("whoHas requires movieId, qualityId and segmentId"));
            return;
        }
        String movieId = movieIdRaw.trim();
        String qualityId = qualityIdRaw.trim();
        String segmentId = segmentIdRaw.trim();
        if (movieId.isEmpty() || qualityId.isEmpty() || segmentId.isEmpty()) {
            send(session, new ErrorMessage("whoHas requires movieId, qualityId and segmentId"));
            return;
        }
        WhoHasReplyMessage reply = signalingService.handleWhoHas(movieId, qualityId, segmentId);
        send(session, reply);
    }

    private void handleReportSegment(WebSocketSession session, JsonNode payload) throws IOException {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String movieIdRaw = payload.path("movieId").textValue();
        String movieId;
        if (movieIdRaw == null) {
            movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
        } else {
            movieId = movieIdRaw.trim();
            if (movieId.isEmpty()) {
                movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
            }
        }
        String qualityIdRaw = payload.path("qualityId").textValue();
        if (qualityIdRaw == null) {
            send(session, new ErrorMessage("reportSegment requires qualityId"));
            return;
        }
        String qualityId = qualityIdRaw.trim();
        if (qualityId.isEmpty()) {
            send(session, new ErrorMessage("reportSegment requires qualityId"));
            return;
        }
        String segmentIdRaw = payload.path("segmentId").textValue();
        if (segmentIdRaw == null) {
            send(session, new ErrorMessage("reportSegment requires segmentId"));
            return;
        }
        String segmentId = segmentIdRaw.trim();
        if (segmentId.isEmpty()) {
            send(session, new ErrorMessage("reportSegment requires segmentId"));
            return;
        }
        String source = payload.path("source").textValue();
        if (source == null || source.isEmpty()) {
            send(session, new ErrorMessage("reportSegment requires source"));
            return;
        }
        long latency = payload.path("latency").asLong(0L);
        double speed = payload.path("speed").asDouble(0.0d);
        ReportSegmentAckMessage ack = signalingService.handleReportSegment(clientId, movieId, qualityId, segmentId,
                source, speed, latency);
        send(session, ack);
    }

    private void handleRemoveSegment(WebSocketSession session, JsonNode payload) throws IOException {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String movieIdRaw = payload.path("movieId").textValue();
        String movieId;
        if (movieIdRaw == null) {
            movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
        } else {
            movieId = movieIdRaw.trim();
            if (movieId.isEmpty()) {
                movieId = attribute(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID);
            }
        }
        String qualityIdRaw = payload.path("qualityId").textValue();
        if (qualityIdRaw == null) {
            send(session, new ErrorMessage("removeSegment requires qualityId"));
            return;
        }
        String qualityId = qualityIdRaw.trim();
        if (qualityId.isEmpty()) {
            send(session, new ErrorMessage("removeSegment requires qualityId"));
            return;
        }
        String segmentIdRaw = payload.path("segmentId").textValue();
        if (segmentIdRaw == null) {
            send(session, new ErrorMessage("removeSegment requires segmentId"));
            return;
        }
        String segmentId = segmentIdRaw.trim();
        if (segmentId.isEmpty()) {
            send(session, new ErrorMessage("removeSegment requires segmentId"));
            return;
        }
        signalingService.handleRemoveSegment(clientId, movieId, qualityId, segmentId);
    }

    private void handleRtcRelay(WebSocketSession session, JsonNode payload) throws IOException {
        String from = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String to = payload.path("to").textValue();
        if (to == null || to.isBlank()) {
            send(session, new ErrorMessage("RTC message requires 'to'"));
            return;
        }
        WebSocketSession target = sessions.get(to);
        if (target == null || !target.isOpen()) {
            send(session, new ErrorMessage("Target peer is not connected: " + to));
            return;
        }
        // Forward as-is, enforcing 'from'
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put("from", from);
        String json = objectMapper.writeValueAsString(payload);
        target.sendMessage(new TextMessage(Objects.requireNonNull(json, "Serialized payload must not be null")));
    }

    private <T> void send(WebSocketSession session, T payload) throws IOException {
        String json = objectMapper.writeValueAsString(payload);
        session.sendMessage(new TextMessage(Objects.requireNonNull(json, "Serialized payload must not be null")));
    }

    private @NonNull String attribute(WebSocketSession session, String attributeName) {
        Object value = session.getAttributes().get(attributeName);
        if (value instanceof String stringValue) {
            String trimmedValue = stringValue.trim();
            if (!trimmedValue.isEmpty()) {
                return trimmedValue;
            }
        }
        throw new IllegalStateException("Missing session attribute " + attributeName);
    }
}
