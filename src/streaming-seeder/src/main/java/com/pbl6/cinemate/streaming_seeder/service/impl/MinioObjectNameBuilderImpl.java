package com.pbl6.cinemate.streaming_seeder.service.impl;

import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.config.SeederProperties;
import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey.SegmentType;
import com.pbl6.cinemate.streaming_seeder.service.MinioObjectNameBuilder;

@Component
public class MinioObjectNameBuilderImpl implements MinioObjectNameBuilder {

    private static final String PATH_SEPARATOR = "/";

    private final SeederProperties properties;

    public MinioObjectNameBuilderImpl(SeederProperties properties) {
        this.properties = properties;
    }

    @Override
    public String buildObjectName(String movieId, String qualityId, String fileName, SegmentType type) {
        String prefix = properties.origin().objectPrefix();
        String basePath = buildBasePath(prefix);

        // Master playlist is at movie level
        if (type == SegmentType.MASTER_PLAYLIST) {
            return basePath + movieId + PATH_SEPARATOR + fileName;
        }

        // All other segments are under quality directory
        if (qualityId == null || qualityId.isBlank()) {
            return basePath + movieId + PATH_SEPARATOR + fileName;
        }

        return basePath + movieId + PATH_SEPARATOR + qualityId + PATH_SEPARATOR + fileName;
    }

    /**
     * Builds the base path from the configured prefix.
     */
    private String buildBasePath(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.replaceAll("/*$", "") + PATH_SEPARATOR;
    }
}
