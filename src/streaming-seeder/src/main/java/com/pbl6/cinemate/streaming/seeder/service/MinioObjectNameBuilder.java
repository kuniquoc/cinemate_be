package com.pbl6.cinemate.streaming.seeder.service;

import com.pbl6.cinemate.streaming.seeder.SegmentKey.SegmentType;
import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
import org.springframework.stereotype.Component;

/**
 * Builds MinIO object names (paths) for segments.
 */
@Component
public class MinioObjectNameBuilder {

    private static final String PATH_SEPARATOR = "/";

    private final SeederProperties properties;

    public MinioObjectNameBuilder(SeederProperties properties) {
        this.properties = properties;
    }

    /**
     * Builds the MinIO object name (path) for a segment.
     * 
     * @param movieId   the movie identifier
     * @param qualityId the quality variant (can be null for master playlist)
     * @param fileName  the complete filename including extension (e.g.,
     *                  "seg_0005.m4s")
     * @param type      the segment type
     * @return the complete MinIO object path
     */
    public String buildObjectName(String movieId, String qualityId, String fileName, SegmentType type) {
        String prefix = properties.getOrigin().getObjectPrefix();
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
