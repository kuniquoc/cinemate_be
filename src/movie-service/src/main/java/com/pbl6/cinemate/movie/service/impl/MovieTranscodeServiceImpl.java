package com.pbl6.cinemate.movie.service.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.MovieProcessStatus;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.FFmpegService;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.MovieTranscodeService;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieTranscodeServiceImpl implements MovieTranscodeService {
    private final FFmpegService ffmpeg;
    private final MinioStorageService minio;
    private final MovieRepository repo;

    @Value("${minio.movie-bucket:}")
    private String movieBucket;

    public MovieTranscodeServiceImpl(FFmpegService ffmpeg, MinioStorageService minio,
                                     MovieRepository repo) {
        this.ffmpeg = ffmpeg;
        this.minio = minio;
        this.repo = repo;
    }

    @Transactional
    public void transcodeMovie(@NonNull UUID movieId, Path inputFile) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        try {
            movie.setProcessStatus(MovieProcessStatus.PROCESSING);
            repo.save(movie);

            // Get video metadata to determine max quality
            FFmpegService.VideoMetadata metadata = ffmpeg.getVideoMetadata(inputFile);
            log.info("Video metadata for movie {}: {}x{}, bitrate: {}",
                    movieId, metadata.width(), metadata.height(), metadata.bitrate());

            // Define all possible variants (max 1080p)
            List<FFmpegService.Variant> allVariants = List.of(
                    new FFmpegService.Variant("360p", "640x360", "500k", "96k", 800_000),
                    new FFmpegService.Variant("480p", "854x480", "1400k", "128k", 1400_000),
                    new FFmpegService.Variant("720p", "1280x720", "2500k", "128k", 2500_000),
                    new FFmpegService.Variant("1080p", "1920x1080", "4000k", "192k", 5000_000));

            // Filter variants based on video resolution
            // Only transcode to qualities <= source quality (max 1080p)
            int maxHeight = Math.min(metadata.height(), 1080);
            List<FFmpegService.Variant> variants = allVariants.stream()
                    .filter(v -> {
                        int variantHeight = Integer.parseInt(v.resolution().split("x")[1]);
                        return variantHeight <= maxHeight;
                    })
                    .toList();

            if (variants.isEmpty()) {
                // If no variants match (e.g., video is smaller than 360p), use the smallest
                // variant
                variants = List.of(allVariants.get(0));
            }

            log.info("Transcoding movie {} to {} variants: {}",
                    movieId, variants.size(),
                    variants.stream().map(FFmpegService.Variant::name).toList());

            ffmpeg.transcode(inputFile, movieId, variants);
            Path baseFolder = Paths.get("/tmp/movies", String.valueOf(movieId));
            String basePath = String.join("/", "hls", movieId.toString());
            String uploadPrefix = basePath + "/";
            minio.uploadFolder(baseFolder.toFile(), movieBucket, uploadPrefix);

            List<String> qualities = variants.stream()
                    .map(FFmpegService.Variant::name)
                    .toList();
            movie.setQualities(qualities);
            movie.setProcessStatus(MovieProcessStatus.COMPLETED);
            movie.setStatus(MovieStatus.PRIVATE); // Set to PRIVATE after successful transcoding
            repo.save(movie);

            log.info("Successfully transcoded movie with id: {}", movieId);
        } catch (Exception e) {
            log.error("Failed to transcode movie with id: {}", movieId, e);
            movie.setProcessStatus(MovieProcessStatus.FAILED);
            repo.save(movie);
        }
    }
}
