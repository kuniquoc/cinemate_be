package com.pbl6.cinemate.streaming.seeder;

import com.pbl6.cinemate.streaming.seeder.config.SeederProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OriginSegmentFetcher {

    private static final Logger log = LoggerFactory.getLogger(OriginSegmentFetcher.class);
    private final MinioClient minioClient;
    private final SeederProperties properties;
    private final Clock clock;
    private final String defaultBucket;

    public OriginSegmentFetcher(
            MinioClient minioClient,
            SeederProperties properties,
            Clock clock,
            @Value("${minio.bucket}") String defaultBucket) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.clock = clock;
        this.defaultBucket = defaultBucket;
    }

    public Optional<CachedSegment> fetchFromOrigin(String streamId, String segmentId) {
        if (!properties.getOrigin().isEnabled()) {
            return Optional.empty();
        }
        StreamDescriptor descriptor = parseStreamId(streamId);
        if (descriptor == null) {
            log.debug("Cannot parse streamId {} for origin fetch", streamId);
            return Optional.empty();
        }
        String sanitizedSegmentId = sanitize(segmentId);
        if (sanitizedSegmentId == null) {
            log.debug("Segment id {} is not valid for origin fetch", segmentId);
            return Optional.empty();
        }

        Path streamDir = properties.getCachePath().resolve(streamId);
        try {
            Files.createDirectories(streamDir);
        } catch (IOException ex) {
            log.warn("Failed to prepare cache directory {}: {}", streamDir, ex.getMessage());
            return Optional.empty();
        }

        String bucket = resolveBucket();
        List<String> extensions = properties.getOrigin().getSegmentExtensions();
        if (extensions == null || extensions.isEmpty()) {
            log.warn("Không có segment extension nào được cấu hình cho origin; bỏ qua đồng bộ");
            return Optional.empty();
        }
        for (String extension : extensions) {
            String fileName = sanitizedSegmentId + "." + extension;
            String objectName = buildObjectName(descriptor, fileName);
            try (InputStream objectStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build())) {
                Path target = streamDir.resolve(fileName);
                Files.copy(objectStream, target, StandardCopyOption.REPLACE_EXISTING);
                Instant now = Instant.now(clock);
                Files.setLastModifiedTime(target, FileTime.from(now));
                log.info("Fetched segment {} for stream {} from origin {}", sanitizedSegmentId, streamId, objectName);
                return Optional.of(new CachedSegment(streamId, sanitizedSegmentId, target, now));
            } catch (ErrorResponseException ex) {
                if (isNotFound(ex)) {
                    log.debug("Segment {} not present at {}/{}", fileName, bucket, objectName);
                    continue;
                }
                log.warn("Origin request for {}/{} failed: {}", bucket, objectName, ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to write segment {} from origin: {}", objectName, ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private String resolveBucket() {
        String configured = properties.getOrigin().getBucket();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return defaultBucket;
    }

    private String buildObjectName(StreamDescriptor descriptor, String fileName) {
        String prefix = properties.getOrigin().getObjectPrefix();
        if (prefix == null || prefix.isBlank()) {
            return descriptor.movieId() + "/" + descriptor.quality() + "/" + fileName;
        }
        return prefix.replaceAll("/*$", "") + "/" + descriptor.movieId() + "/" + descriptor.quality() + "/" + fileName;
    }

    private StreamDescriptor parseStreamId(String streamId) {
        if (streamId == null) {
            return null;
        }
        int separator = streamId.indexOf('_');
        if (separator <= 0 || separator >= streamId.length() - 1) {
            return null;
        }
        String moviePart = streamId.substring(0, separator);
        String qualityPart = streamId.substring(separator + 1);
        if (!isValidUuid(moviePart) || !isSafeComponent(qualityPart)) {
            return null;
        }
        return new StreamDescriptor(moviePart, qualityPart);
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isSafeComponent(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return !value.contains("..") && !value.contains("/") && !value.contains("\\");
    }

    private String sanitize(String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return null;
        }
        String trimmed = segmentId.trim();
        int firstDot = trimmed.indexOf('.');
        if (firstDot > 0) {
            trimmed = trimmed.substring(0, firstDot);
        }
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            return null;
        }
        return trimmed;
    }

    private boolean isNotFound(ErrorResponseException ex) {
        return "NoSuchKey".equalsIgnoreCase(ex.errorResponse().code())
                || ex.errorResponse().code() == null;
    }

    private record StreamDescriptor(String movieId, String quality) {
    }
}
