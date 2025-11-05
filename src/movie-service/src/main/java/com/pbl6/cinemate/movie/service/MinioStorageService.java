package com.pbl6.cinemate.movie.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface MinioStorageService {
    String save(File file, String objectPath);

    void uploadFolder(File folder, String objectPrefix);

    String saveChunk(File chunkFile, String uploadId, Integer chunkNumber);

    String saveChunk(InputStream inputStream, long size, String uploadId, Integer chunkNumber);

    String composeChunks(String uploadId, String finalObjectPath, int totalChunks);

    void cleanupChunks(String uploadId);

    boolean chunkExists(String uploadId, Integer chunkNumber);

    List<Integer> getExistingChunks(String uploadId);

    InputStream getObject(String objectPath);

    String getPublicUrl(String objectPath);
}
