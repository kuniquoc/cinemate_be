package com.pbl6.cinemate.streaming_signaling.dto;

public record PeerMetrics(double uploadSpeed, int latency, double successRate, long lastActive) {
}
