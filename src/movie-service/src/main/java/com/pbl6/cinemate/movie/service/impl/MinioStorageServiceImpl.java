package com.pbl6.cinemate.movie.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.shared.exception.InternalServerException;

import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.messages.Item;

@Slf4j
@Service
public class MinioStorageServiceImpl implements MinioStorageService {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String CHUNKS_PREFIX = "chunks/";
    private static final String CHUNK_PREFIX = "/chunk_";

    private final MinioClient minioClient;
    private final String movieBucket;
    private final String imageBucket;
    private final String videoBucket;

    public MinioStorageServiceImpl(MinioClient minioClient,
                                   @Value("${minio.movie-bucket}") String movieBucket,
                                   @Value("${minio.image-bucket}") String imageBucket,
                                   @Value("${minio.video-bucket}") String videoBucket) {
        this.minioClient = minioClient;
        this.movieBucket = movieBucket;
        this.imageBucket = imageBucket;
        this.videoBucket = videoBucket;
    }

    @PostConstruct
    public void initializeBuckets() {
        try {
            Set<String> buckets = new LinkedHashSet<>();
            if (movieBucket != null && !movieBucket.isBlank()) {
                buckets.add(movieBucket);
            }
            if (imageBucket != null && !imageBucket.isBlank()) {
                buckets.add(imageBucket);
            }
            if (videoBucket != null && !videoBucket.isBlank()) {
                buckets.add(videoBucket);
            }

            for (String currentBucket : buckets) {
                ensureBucketExists(currentBucket);
                setBucketPublicPolicy(currentBucket);
            }
        } catch (Exception e) {
            throw new InternalServerException("Failed to initialize MinIO buckets: " + e.getMessage());
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("Created MinIO bucket: {}", bucketName);
        } else {
            log.info("MinIO bucket already exists: {}", bucketName);
        }
    }

    /**
     * Set bucket policy to allow public read access
     */
    private void setBucketPublicPolicy(String bucketName) {
        try {
            String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": "*",
                                "Action": "s3:GetObject",
                                "Resource": "arn:aws:s3:::%s/*"
                            }
                        ]
                    }
                    """.formatted(bucketName);

            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policy)
                    .build());

            log.info("Set public read policy for MinIO bucket: {}", bucketName);
        } catch (Exception e) {
            log.warn("Failed to set bucket policy (this might be expected in some MinIO configurations): {}",
                    e.getMessage());
        }
    }

    public String save(File file, String bucketName, String objectPath) {
        try (FileInputStream fis = new FileInputStream(file)) {
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                // Fallback content type determination based on file extension
                contentType = determineContentTypeFromExtension(file.getName());
            }

            String resolvedBucket = effectiveBucket(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(objectPath)
                    .stream(fis, file.length(), -1)
                    .contentType(contentType)
                    .build());
            return getPublicUrl(resolvedBucket, objectPath);
        } catch (Exception e) {
            throw new InternalServerException("Failed to save file to MinIO: " + e.getMessage());
        }
    }

    public void uploadFolder(File folder, String bucketName, String objectPrefix) {
        File[] files = folder.listFiles();
        if (files == null) {
            throw new InternalServerException("Cannot list files in folder: " + folder.getPath());
        }

        for (File f : files) {
            if (f.isDirectory()) {
                uploadFolder(f, bucketName, objectPrefix + f.getName() + "/");
            } else {
                save(f, bucketName, objectPrefix + f.getName());
            }
        }
    }

    /**
     * Save a chunk file to MinIO
     */
    public String saveChunk(File chunkFile, String bucketName, String uploadId, Integer chunkNumber) {
        String objectPath = CHUNKS_PREFIX + uploadId + CHUNK_PREFIX + chunkNumber;
        return save(chunkFile, bucketName, objectPath);
    }

    /**
     * Save chunk from input stream
     */
    public String saveChunk(InputStream inputStream, long size, String bucketName, String uploadId,
                            Integer chunkNumber) {
        String objectPath = CHUNKS_PREFIX + uploadId + CHUNK_PREFIX + chunkNumber;
        try {
            String resolvedBucket = effectiveBucket(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(objectPath)
                    .stream(inputStream, size, -1)
                    .contentType(DEFAULT_CONTENT_TYPE)
                    .build());
            return getPublicUrl(resolvedBucket, objectPath);
        } catch (Exception e) {
            throw new InternalServerException("Failed to save chunk to MinIO: " + e.getMessage());
        }
    }

    /**
     * Compose chunks into final file using MinIO's server-side composition
     */
    public String composeChunks(String bucketName, String uploadId, String finalObjectPath, int totalChunks) {
        try {
            List<ComposeSource> sources = new ArrayList<>();
            String resolvedBucket = effectiveBucket(bucketName);

            // Add all chunks as sources
            for (int i = 0; i < totalChunks; i++) {
                String chunkPath = CHUNKS_PREFIX + uploadId + CHUNK_PREFIX + i;
                sources.add(ComposeSource.builder()
                        .bucket(resolvedBucket)
                        .object(chunkPath)
                        .build());
            }

            // Compose chunks into final object
            minioClient.composeObject(ComposeObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(finalObjectPath)
                    .sources(sources)
                    .build());

            return getPublicUrl(resolvedBucket, finalObjectPath);
        } catch (Exception e) {
            throw new InternalServerException("Failed to compose chunks in MinIO: " + e.getMessage());
        }
    }

    /**
     * Clean up chunk files from MinIO
     */
    public void cleanupChunks(String bucketName, String uploadId) {
        try {
            String prefix = CHUNKS_PREFIX + uploadId + "/";
            String resolvedBucket = effectiveBucket(bucketName);

            // List all chunk objects
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(resolvedBucket)
                            .prefix(prefix)
                            .build());

            // Delete each chunk
            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(resolvedBucket)
                        .object(item.objectName())
                        .build());
            }
        } catch (Exception e) {
            throw new InternalServerException("Failed to cleanup chunks from MinIO: " + e.getMessage());
        }
    }

    /**
     * Check if chunk exists in MinIO
     */
    public boolean chunkExists(String bucketName, String uploadId, Integer chunkNumber) {
        try {
            String objectPath = CHUNKS_PREFIX + uploadId + CHUNK_PREFIX + chunkNumber;
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(effectiveBucket(bucketName))
                    .object(objectPath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get list of existing chunks for an upload
     */
    public List<Integer> getExistingChunks(String bucketName, String uploadId) {
        try {
            String prefix = CHUNKS_PREFIX + uploadId + "/";
            List<Integer> existingChunks = new ArrayList<>();
            String resolvedBucket = effectiveBucket(bucketName);

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(resolvedBucket)
                            .prefix(prefix)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                // Extract chunk number from object name
                String chunkFileName = objectName.substring(objectName.lastIndexOf('/') + 1);
                if (chunkFileName.startsWith("chunk_")) {
                    Integer chunkNumber = parseChunkNumber(chunkFileName);
                    if (chunkNumber != null) {
                        existingChunks.add(chunkNumber);
                    }
                }
            }

            return existingChunks;
        } catch (Exception e) {
            throw new InternalServerException("Failed to list existing chunks: " + e.getMessage());
        }
    }

    /**
     * Get object as InputStream
     */
    public InputStream getObject(String bucketName, String objectPath) {
        try {
            String resolvedBucket = effectiveBucket(bucketName);
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            throw new InternalServerException("Failed to get object from MinIO: " + e.getMessage());
        }
    }

    /**
     * Determine content type from file extension when Files.probeContentType fails
     */
    private String determineContentTypeFromExtension(String filename) {
        if (filename == null) {
            return DEFAULT_CONTENT_TYPE;
        }

        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        }

        return switch (extension) {
            // Video formats
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "avi" -> "video/x-msvideo";
            case "mov" -> "video/quicktime";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "mkv" -> "video/x-matroska";
            case "m4v" -> "video/x-m4v";

            // HLS streaming formats
            case "m3u8" -> "application/vnd.apple.mpegurl";
            case "ts" -> "video/mp2t";
            case "m4s" -> "video/iso.segment";

            // DASH streaming formats
            case "mpd" -> "application/dash+xml";

            // Audio formats
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            case "m4a" -> "audio/x-m4a";

            // Image formats
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";

            // Text formats
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";

            // Default fallback
            default -> DEFAULT_CONTENT_TYPE;
        };
    }

    /**
     * Get public URL for an object in the bucket
     */
    public String getPublicUrl(String bucketName, String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return effectiveBucket(bucketName) + "/";
        }
        String normalized = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
        String resolvedBucket = effectiveBucket(bucketName);
        return resolvedBucket + "/" + normalized;
    }

    /**
     * Parse chunk number from chunk file name
     *
     * @param chunkFileName the chunk file name (e.g., "chunk_1")
     * @return chunk number or null if parsing fails
     */
    private Integer parseChunkNumber(String chunkFileName) {
        try {
            return Integer.parseInt(chunkFileName.substring(6));
        } catch (NumberFormatException e) {
            // Skip invalid chunk names
            return null;
        }
    }

    private String effectiveBucket(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            return movieBucket;
        }
        return bucketName;
    }
}
