package com.pbl6.cinemate.streaming.dto;

public record ReportSegmentAckMessage(String type, String segmentId, String status) {

    public ReportSegmentAckMessage(String segmentId) {
        this("REPORT_SEGMENT_ACK", segmentId, "OK");
    }
}
