package com.pbl6.cinemate.movie.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface MinioStorageService {
    String save(File file, String bucketName, String objectPath);

    void uploadFolder(File folder, String bucketName, String objectPrefix);

    String saveChunk(File chunkFile, String bucketName, String uploadId, Integer chunkNumber);

    String saveChunk(InputStream inputStream, long size, String bucketName, String uploadId, Integer chunkNumber);

    String composeChunks(String bucketName, String uploadId, String finalObjectPath, int totalChunks);

    void cleanupChunks(String bucketName, String uploadId);

    boolean chunkExists(String bucketName, String uploadId, Integer chunkNumber);

    List<Integer> getExistingChunks(String bucketName, String uploadId);

    InputStream getObject(String bucketName, String objectPath);

    String getPublicUrl(String bucketName, String objectPath);
}
