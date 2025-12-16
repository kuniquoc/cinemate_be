package com.pbl6.cinemate.streaming_seeder.service;

import java.nio.file.Path;

/**
 * Strategy for determining HTTP Cache-Control headers for segments.
 */
public interface CacheControlStrategy {

    /**
     * Returns the Cache-Control header value for the given file path.
     *
     * @param path the file path
     * @return the Cache-Control header value
     */
    String getCacheControl(Path path);
}
