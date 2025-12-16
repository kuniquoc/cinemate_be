package com.pbl6.cinemate.streaming_seeder.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Serves segment files as HTTP responses.
 */
public interface SegmentFileServer {

    /**
     * Serves a file as an HTTP response with appropriate headers.
     *
     * @param path the file path to serve
     * @return the HTTP response containing the file
     * @throws IOException if an I/O error occurs
     */
    ResponseEntity<Resource> serve(Path path) throws IOException;
}
