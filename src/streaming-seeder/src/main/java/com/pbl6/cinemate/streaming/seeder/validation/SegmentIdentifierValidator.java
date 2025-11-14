package com.pbl6.cinemate.streaming.seeder.validation;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Validates segment identifiers to prevent path traversal attacks and ensure
 * safe file system operations.
 */
@Component
public class SegmentIdentifierValidator {

    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_TRAVERSAL = "..";

    /**
     * Checks if a value is safe to use as a path component.
     */
    public boolean isSafeIdentifier(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains(PATH_TRAVERSAL)
                && !value.contains(PATH_SEPARATOR)
                && !value.contains("\\");
    }

    /**
     * Validates if the movie ID is safe to use (UUID or safe string).
     */
    public boolean isValidMovieId(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            return false;
        }

        // Try to parse as UUID first
        try {
            UUID.fromString(movieId);
            return true;
        } catch (IllegalArgumentException ex) {
            // Not a UUID, check if it's a safe component
            return isSafeIdentifier(movieId);
        }
    }
}
