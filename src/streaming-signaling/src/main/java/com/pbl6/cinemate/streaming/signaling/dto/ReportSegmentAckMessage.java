package com.pbl6.cinemate.streaming.signaling.dto;

public record ReportSegmentAckMessage(String type, String segmentId) {

    public ReportSegmentAckMessage(String segmentId) {
        this("REPORT_ACK", segmentId);
    }
}
