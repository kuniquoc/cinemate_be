package com.pbl6.cinemate.streaming_seeder.service.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.streaming_seeder.config.SeederProperties;
import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;
import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey;
import com.pbl6.cinemate.streaming_seeder.dto.SegmentKey.SegmentType;
import com.pbl6.cinemate.streaming_seeder.service.MinioObjectNameBuilder;
import com.pbl6.cinemate.streaming_seeder.service.OriginSegmentFetcher;
import com.pbl6.cinemate.streaming_seeder.service.SegmentCacheWriter;
import com.pbl6.cinemate.streaming_seeder.service.SegmentIdNormalizer;
import com.pbl6.cinemate.streaming_seeder.validation.SegmentIdentifierValidator;

@Component
public class OriginSegmentFetcherImpl implements OriginSegmentFetcher {

    private static final Logger log = LoggerFactory.getLogger(OriginSegmentFetcherImpl.class);

    // MinIO error codes
    private static final String ERROR_NO_SUCH_KEY = "NoSuchKey";

    private final MinioClient minioClient;
    private final SeederProperties properties;
    private final SegmentIdentifierValidator validator;
    private final SegmentIdNormalizer normalizer;
    private final MinioObjectNameBuilder objectNameBuilder;
    private final SegmentCacheWriter cacheWriter;
    private final String defaultBucket;

    /**
     * Internal record to hold fetch context parameters.
     */
    private record FetchContext(
            String movieId,
            String qualityId,
            String sanitizedSegmentId,
            SegmentType type,
            String bucket) {
    }

    public OriginSegmentFetcherImpl(
            MinioClient minioClient,
            SeederProperties properties,
            SegmentIdentifierValidator validator,
            SegmentIdNormalizer normalizer,
            MinioObjectNameBuilder objectNameBuilder,
            SegmentCacheWriter cacheWriter,
            @Value("${minio.bucket}") String defaultBucket) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.validator = validator;
        this.normalizer = normalizer;
        this.objectNameBuilder = objectNameBuilder;
        this.cacheWriter = cacheWriter;
        this.defaultBucket = defaultBucket;
    }

    @Override
    public Optional<CachedSegment> fetchFromOrigin(String movieId, String qualityId, String segmentId) {
        if (!isOriginEnabled()) {
            return Optional.empty();
        }

        if (!validator.isValidMovieId(movieId)) {
            log.debug("Invalid movieId '{}' for origin fetch", movieId);
            return Optional.empty();
        }

        String sanitizedSegmentId = normalizer.sanitize(segmentId);
        if (sanitizedSegmentId == null || !validator.isSafeIdentifier(sanitizedSegmentId)) {
            log.debug("Invalid segment id '{}' for origin fetch", segmentId);
            return Optional.empty();
        }

        SegmentKey key = new SegmentKey(movieId, qualityId, sanitizedSegmentId);
        SegmentType type = key.getType();
        String bucket = resolveBucket();

        return fetchSegmentWithExtensions(movieId, qualityId, sanitizedSegmentId, type, bucket);
    }

    /**
     * Attempts to fetch the segment by trying different file extensions.
     */
    private Optional<CachedSegment> fetchSegmentWithExtensions(
            String movieId,
            String qualityId,
            String sanitizedSegmentId,
            SegmentType type,
            String bucket) {

        List<String> extensions = getExtensionsForType(type);
        FetchContext context = new FetchContext(movieId, qualityId, sanitizedSegmentId, type, bucket);

        for (String extension : extensions) {
            // SegmentId is now the complete filename, but we still try different extensions
            // for non-media segments (init, playlists)
            String fileName = sanitizedSegmentId.endsWith("." + extension)
                    ? sanitizedSegmentId
                    : sanitizedSegmentId + "." + extension;
            String objectName = objectNameBuilder.buildObjectName(movieId, qualityId, fileName, type);

            Optional<CachedSegment> segment = fetchSingleSegment(context, fileName, objectName);

            if (segment.isPresent()) {
                return segment;
            }
        }

        return Optional.empty();
    }

    /**
     * Fetches a single segment from MinIO and caches it locally.
     */
    private Optional<CachedSegment> fetchSingleSegment(FetchContext context, String fileName, String objectName) {
        try (InputStream objectStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(context.bucket())
                .object(objectName)
                .build())) {

            CachedSegment segment = cacheWriter.saveToCache(
                    context.movieId(),
                    context.qualityId(),
                    context.sanitizedSegmentId(),
                    fileName,
                    context.type(),
                    objectStream);

            log.info("Fetched {} segment '{}' for movie '{}' quality '{}' from origin '{}'",
                    context.type(), context.sanitizedSegmentId(), context.movieId(),
                    context.qualityId(), objectName);

            return Optional.of(segment);

        } catch (ErrorResponseException ex) {
            handleMinioError(ex, context.bucket(), objectName, fileName);
        } catch (Exception ex) {
            log.warn("Failed to fetch segment '{}' from origin: {}", objectName, ex.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Handles MinIO-specific errors during segment fetching.
     */
    private void handleMinioError(ErrorResponseException ex, String bucket, String objectName, String fileName) {
        if (isNotFound(ex)) {
            log.debug("Segment '{}' not found at {}/{}", fileName, bucket, objectName);
        } else {
            log.warn("MinIO request failed for {}/{}: {}", bucket, objectName, ex.getMessage());
        }
    }

    /**
     * Checks if origin fetching is enabled.
     */
    private boolean isOriginEnabled() {
        return properties.origin().enabled();
    }

    /**
     * Returns the list of file extensions to try for each segment type.
     */
    private List<String> getExtensionsForType(SegmentType type) {
        return switch (type) {
            case INIT -> List.of("mp4", "m4s"); // Prioritize .mp4 for init
            case MASTER_PLAYLIST, VARIANT_PLAYLIST -> List.of("m3u8");
            case MEDIA -> List.of("m4s", "mp4"); // fMP4 media segments
        };
    }

    /**
     * Resolves the bucket name to use.
     */
    private String resolveBucket() {
        String configured = properties.origin().bucket();
        return (configured != null && !configured.isBlank()) ? configured : defaultBucket;
    }

    /**
     * Checks if the error response indicates a "not found" error.
     */
    private boolean isNotFound(ErrorResponseException ex) {
        return ERROR_NO_SUCH_KEY.equalsIgnoreCase(ex.errorResponse().code())
                || ex.errorResponse().code() == null;
    }
}
