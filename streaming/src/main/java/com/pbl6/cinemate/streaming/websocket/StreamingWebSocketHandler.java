package com.pbl6.cinemate.streaming.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.streaming.dto.ErrorMessage;
import com.pbl6.cinemate.streaming.dto.PeerListMessage;
import com.pbl6.cinemate.streaming.dto.ReportSegmentAckMessage;
import com.pbl6.cinemate.streaming.dto.WhoHasReplyMessage;
import com.pbl6.cinemate.streaming.service.SignalingService;

import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public StreamingWebSocketHandler(SignalingService signalingService, ObjectMapper objectMapper) {
        this.signalingService = signalingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String streamId = attribute(session, SignalingHandshakeInterceptor.ATTR_STREAM_ID);
        PeerListMessage peerListMessage = signalingService.registerClient(clientId, streamId);
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
            case "WHO_HAS" -> handleWhoHas(session, payload);
            case "REPORT_SEGMENT" -> handleReportSegment(session, payload);
            default -> send(session, new ErrorMessage("Unsupported message type: " + type));
        }
        log.debug("Handled {} message from client {}", type, clientId);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String streamId = attribute(session, SignalingHandshakeInterceptor.ATTR_STREAM_ID);
        signalingService.handleDisconnect(clientId, streamId);
    }

    private void handleWhoHas(WebSocketSession session, JsonNode payload) throws IOException {
        String streamIdRaw = payload.path("streamId").textValue();
        String segmentIdRaw = payload.path("segmentId").textValue();
        if (streamIdRaw == null || segmentIdRaw == null) {
            send(session, new ErrorMessage("WHO_HAS requires streamId and segmentId"));
            return;
        }
        String streamId = streamIdRaw.trim();
        String segmentId = segmentIdRaw.trim();
        if (streamId.isEmpty() || segmentId.isEmpty()) {
            send(session, new ErrorMessage("WHO_HAS requires streamId and segmentId"));
            return;
        }
        WhoHasReplyMessage reply = signalingService.handleWhoHas(streamId, segmentId);
        send(session, reply);
    }

    private void handleReportSegment(WebSocketSession session, JsonNode payload) throws IOException {
        String clientId = attribute(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID);
        String streamIdRaw = payload.path("streamId").textValue();
        String streamId;
        if (streamIdRaw == null) {
            streamId = attribute(session, SignalingHandshakeInterceptor.ATTR_STREAM_ID);
        } else {
            streamId = streamIdRaw.trim();
            if (streamId.isEmpty()) {
                streamId = attribute(session, SignalingHandshakeInterceptor.ATTR_STREAM_ID);
            }
        }
        String segmentIdRaw = payload.path("segmentId").textValue();
        if (segmentIdRaw == null) {
            send(session, new ErrorMessage("REPORT_SEGMENT requires segmentId"));
            return;
        }
        String segmentId = segmentIdRaw.trim();
        if (segmentId.isEmpty()) {
            send(session, new ErrorMessage("REPORT_SEGMENT requires segmentId"));
            return;
        }
        String sourceRaw = payload.path("source").textValue();
        String source = sourceRaw == null ? "peer" : sourceRaw.trim();
        if (source.isEmpty()) {
            source = "peer";
        }
        long latency = payload.path("latency").asLong(0L);
        double speed = payload.path("speed").asDouble(0.0d);
        ReportSegmentAckMessage ack = signalingService.handleReportSegment(clientId, streamId, segmentId, source, speed,
                latency);
        send(session, ack);
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
