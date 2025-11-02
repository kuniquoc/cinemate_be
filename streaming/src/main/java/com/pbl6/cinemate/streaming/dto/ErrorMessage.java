package com.pbl6.cinemate.streaming.dto;

public record ErrorMessage(String type, String message) {

    public ErrorMessage(String message) {
        this("ERROR", message);
    }
}
