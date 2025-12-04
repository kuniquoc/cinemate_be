package com.pbl6.cinemate.streaming_signaling.util;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class JsonHelper {

    public String getText(JsonNode json, String key, boolean required) {
        JsonNode node = json.path(key);
        if (!node.isTextual()) {
            if (required)
                throw new IllegalArgumentException("Missing field: " + key);
            return null;
        }
        String v = node.textValue().trim();
        if (required && v.isEmpty())
            throw new IllegalArgumentException("Missing field: " + key);
        return v;
    }

    public long getLong(JsonNode json, String key, long defaultVal) {
        return json.path(key).asLong(defaultVal);
    }

    public double getDouble(JsonNode json, String key, double defaultVal) {
        return json.path(key).asDouble(defaultVal);
    }
}
