package com.pbl6.cinemate.streaming_seeder.service;

import java.nio.file.Path;
import org.springframework.http.MediaType;

/**
 * Detects media types for streaming segments.
 */
public interface MediaTypeDetector {

    /**
     * Detects the media type of a file.
     *
     * @param path the file path
     * @return the detected MediaType
     */
    MediaType detect(Path path);
}
