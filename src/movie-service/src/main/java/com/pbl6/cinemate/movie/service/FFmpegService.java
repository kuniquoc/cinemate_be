package com.pbl6.cinemate.movie.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FFmpegService {
    Map<String, Path> transcode(Path inputFile, UUID movieId, List<Variant> variants);

    VideoMetadata getVideoMetadata(Path inputFile);

    record Variant(String name, String resolution,
                   String videoBitrate, String audioBitrate, int bandwidth) {
    }

    record VideoMetadata(int width, int height, long bitrate) {
    }
}
