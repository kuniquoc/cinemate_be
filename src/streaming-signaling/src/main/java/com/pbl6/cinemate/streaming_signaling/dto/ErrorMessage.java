package com.pbl6.cinemate.streaming_signaling.dto;

public record ErrorMessage(String type, String message) {

    public ErrorMessage(String message) {
        this("error", message);
    }
}
