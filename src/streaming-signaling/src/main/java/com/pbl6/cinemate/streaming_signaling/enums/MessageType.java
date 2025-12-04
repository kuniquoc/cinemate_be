package com.pbl6.cinemate.streaming_signaling.enums;

import java.util.Arrays;
import java.util.Optional;

public enum MessageType {
    WHO_HAS("whoHas"),
    REPORT_SEGMENT("reportSegment"),
    REMOVE_SEGMENT("removeSegment"),
    RTC_OFFER("rtcOffer"),
    RTC_ANSWER("rtcAnswer"),
    ICE_CANDIDATE("iceCandidate");

    public final String value;

    MessageType(String v) {
        this.value = v;
    }

    public static Optional<MessageType> from(String raw) {
        if (raw == null)
            return Optional.empty();
        String t = raw.trim();
        return Arrays.stream(values())
                .filter(v -> v.value.equals(t))
                .findFirst();
    }
}
