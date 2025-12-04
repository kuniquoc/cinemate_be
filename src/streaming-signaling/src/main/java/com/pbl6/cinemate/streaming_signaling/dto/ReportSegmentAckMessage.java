package com.pbl6.cinemate.streaming_signaling.dto;

public record ReportSegmentAckMessage(String type, String segmentId) {

    public ReportSegmentAckMessage(String segmentId) {
        this("reportAck", segmentId);
    }
}
