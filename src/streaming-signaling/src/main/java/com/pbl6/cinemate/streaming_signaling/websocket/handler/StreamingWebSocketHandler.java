package com.pbl6.cinemate.streaming_signaling.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pbl6.cinemate.streaming_signaling.enums.MessageType;
import com.pbl6.cinemate.streaming_signaling.dto.ErrorMessage;
import com.pbl6.cinemate.streaming_signaling.dto.PeerListMessage;
import com.pbl6.cinemate.streaming_signaling.dto.ReportSegmentAckMessage;
import com.pbl6.cinemate.streaming_signaling.dto.WhoHasReplyMessage;
import com.pbl6.cinemate.streaming_signaling.service.SignalingService;
import com.pbl6.cinemate.streaming_signaling.websocket.interceptor.SignalingHandshakeInterceptor;
import com.pbl6.cinemate.streaming_signaling.util.JsonHelper;
import com.pbl6.cinemate.streaming_signaling.util.websocket.Attrs;

import java.io.IOException;
import java.util.Objects;
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
    private final SignalingService signalingService;
    private final ObjectMapper objectMapper;
    private final JsonHelper jsonHelper;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final String CLIENT_ID_NULL = "Client ID cannot be null";
    private static final String MOVIE_ID_NULL = "Movie ID cannot be null";
    private static final String QUALITY_ID_NULL = "Quality ID cannot be null";
    private static final String SEGMENT_ID_NULL = "Segment ID cannot be null";
    private static final String TO_NULL = "Target peer ID cannot be null";
    private static final String SOURCE_NULL = "Source cannot be null";
    private static final String LATENCY_NULL = "Latency cannot be null";
    private static final String SPEED_NULL = "Speed cannot be null";
    private static final String MOVIE_ID_KEY = "movieId";
    private static final String QUALITY_ID_KEY = "qualityId";
    private static final String SEGMENT_ID_KEY = "segmentId";
    private static final String TO_KEY = "to";

    public StreamingWebSocketHandler(
            SignalingService signalingService,
            ObjectMapper objectMapper,
            JsonHelper jsonHelper) {
        this.signalingService = signalingService;
        this.objectMapper = objectMapper;
        this.jsonHelper = jsonHelper;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String clientId = Objects.requireNonNull(Attrs.get(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID),
                CLIENT_ID_NULL);
        String movieId = Objects.requireNonNull(Attrs.get(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID),
                MOVIE_ID_NULL);

        sessions.put(clientId, session);

        PeerListMessage peerList = signalingService.registerClient(clientId, movieId);
        send(session, peerList);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage msg) throws Exception {
        JsonNode json = objectMapper.readTree(msg.getPayload());

        String typeRaw = json.path("type").textValue();
        MessageType type = MessageType.from(typeRaw)
                .orElse(null);

        if (type == null) {
            send(session, new ErrorMessage("Unsupported or missing message type"));
            return;
        }

        switch (type) {
            case WHO_HAS -> handleWhoHas(session, json);
            case REPORT_SEGMENT -> handleReportSegment(session, json);
            case REMOVE_SEGMENT -> handleRemoveSegment(session, json);
            case RTC_OFFER, RTC_ANSWER, ICE_CANDIDATE -> handleRtcRelay(session, json);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String clientId = Objects.requireNonNull(Attrs.get(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID),
                CLIENT_ID_NULL);
        String movieId = Objects.requireNonNull(Attrs.get(session, SignalingHandshakeInterceptor.ATTR_MOVIE_ID),
                MOVIE_ID_NULL);

        signalingService.handleDisconnect(clientId, movieId);
        sessions.remove(clientId);
    }

    private void handleWhoHas(WebSocketSession s, JsonNode json) throws IOException {
        try {
            String movieId = Objects.requireNonNull(jsonHelper.getText(json, MOVIE_ID_KEY, true), MOVIE_ID_NULL);
            String qualityId = Objects.requireNonNull(jsonHelper.getText(json, QUALITY_ID_KEY, true), QUALITY_ID_NULL);
            String segmentId = Objects.requireNonNull(jsonHelper.getText(json, SEGMENT_ID_KEY, true), SEGMENT_ID_NULL);

            WhoHasReplyMessage reply = signalingService.handleWhoHas(movieId, qualityId, segmentId);
            send(s, reply);
        } catch (IllegalArgumentException e) {
            send(s, new ErrorMessage(e.getMessage()));
        }
    }

    private void handleReportSegment(WebSocketSession s, JsonNode json) throws IOException {
        try {
            String clientId = Objects.requireNonNull(Attrs.get(s, SignalingHandshakeInterceptor.ATTR_CLIENT_ID),
                    CLIENT_ID_NULL);
            String movieId = Objects.requireNonNull(jsonHelper.getText(json, MOVIE_ID_KEY, false), MOVIE_ID_NULL);
            String qualityId = Objects.requireNonNull(jsonHelper.getText(json, QUALITY_ID_KEY, true), QUALITY_ID_NULL);
            String segmentId = Objects.requireNonNull(jsonHelper.getText(json, SEGMENT_ID_KEY, true), SEGMENT_ID_NULL);

            String source = Objects.requireNonNull(jsonHelper.getText(json, "source", true), SOURCE_NULL);
            long latency = Objects.requireNonNull(jsonHelper.getLong(json, "latency", 0), LATENCY_NULL);
            double speed = Objects.requireNonNull(jsonHelper.getDouble(json, "speed", 0.0), SPEED_NULL);

            ReportSegmentAckMessage ack = signalingService.handleReportSegment(clientId, movieId, qualityId, segmentId,
                    source, speed, latency);

            send(s, ack);
        } catch (IllegalArgumentException e) {
            send(s, new ErrorMessage(e.getMessage()));
        }
    }

    private void handleRemoveSegment(WebSocketSession s, JsonNode json) throws IOException {
        try {
            String clientId = Objects.requireNonNull(Attrs.get(s, SignalingHandshakeInterceptor.ATTR_CLIENT_ID),
                    CLIENT_ID_NULL);

            String movieId = Objects.requireNonNull(jsonHelper.getText(json, MOVIE_ID_KEY, false), MOVIE_ID_NULL);
            String qualityId = Objects.requireNonNull(jsonHelper.getText(json, QUALITY_ID_KEY, true), QUALITY_ID_NULL);
            String segmentId = Objects.requireNonNull(jsonHelper.getText(json, SEGMENT_ID_KEY, true), SEGMENT_ID_NULL);

            signalingService.handleRemoveSegment(clientId, movieId, qualityId, segmentId);
        } catch (IllegalArgumentException e) {
            send(s, new ErrorMessage(e.getMessage()));
        }
    }

    private void handleRtcRelay(WebSocketSession session, JsonNode json) throws IOException {
        try {
            String from = Objects.requireNonNull(Attrs.get(session, SignalingHandshakeInterceptor.ATTR_CLIENT_ID),
                    CLIENT_ID_NULL);
            String to = Objects.requireNonNull(jsonHelper.getText(json, TO_KEY, true), TO_NULL);

            WebSocketSession target = sessions.get(to);
            if (target == null || !target.isOpen()) {
                send(session, new ErrorMessage("Target peer not connected: " + to));
                return;
            }

            ((ObjectNode) json).put("from", from);
            String jsonString = Objects.requireNonNull(objectMapper.writeValueAsString(json),
                    "JSON string cannot be null");
            target.sendMessage(new TextMessage(jsonString));

        } catch (IllegalArgumentException e) {
            send(session, new ErrorMessage(e.getMessage()));
        }
    }

    private <T> void send(WebSocketSession s, T obj) throws IOException {
        String jsonString = Objects.requireNonNull(objectMapper.writeValueAsString(obj),
                "JSON string cannot be null");
        s.sendMessage(new TextMessage(jsonString));
    }
}
