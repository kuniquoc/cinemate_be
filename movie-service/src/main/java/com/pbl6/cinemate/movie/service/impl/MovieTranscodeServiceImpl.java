package com.pbl6.cinemate.movie.service.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.exception.NotFoundException;
import com.pbl6.cinemate.movie.repository.MovieRepository;
import com.pbl6.cinemate.movie.service.FFmpegService;
import com.pbl6.cinemate.movie.service.MinioStorageService;
import com.pbl6.cinemate.movie.service.MovieTranscodeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MovieTranscodeServiceImpl implements MovieTranscodeService {
    private final FFmpegService ffmpeg;
    private final MinioStorageService minio;
    private final MovieRepository repo;
    private final ObjectMapper mapper;

    public MovieTranscodeServiceImpl(FFmpegService ffmpeg, MinioStorageService minio,
            MovieRepository repo, ObjectMapper mapper) {
        this.ffmpeg = ffmpeg;
        this.minio = minio;
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional
    public void transcodeMovie(UUID movieId, Path inputFile) {
        Movie movie = repo.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        try {
            movie.setStatus(MovieStatus.PROCESSING);
            repo.save(movie);

            List<FFmpegService.Variant> variants = List.of(
                    new FFmpegService.Variant("360p", "640x360", "800k", "96k", 800_000),
                    new FFmpegService.Variant("720p", "1280x720", "2500k", "128k", 2500_000),
                    new FFmpegService.Variant("1080p", "1920x1080", "5000k", "192k", 5000_000));

            ffmpeg.transcode(inputFile, movieId, variants);
            Path baseFolder = Paths.get("/tmp/movies", String.valueOf(movieId));
            String prefix = "movies/" + movieId + "/";
            minio.uploadFolder(baseFolder.toFile(), prefix);

            Map<String, String> qualities = createQualitiesMap(variants, prefix);
            movie.setQualitiesJson(serializeQualities(qualities));
            movie.setStatus(MovieStatus.READY);
            repo.save(movie);

            log.info("Successfully transcoded movie with id: {}", movieId);
        } catch (Exception e) {
            log.error("Failed to transcode movie with id: {}", movieId, e);
            movie.setStatus(MovieStatus.FAILED);
            repo.save(movie);
        }
    }

    private Map<String, String> createQualitiesMap(List<FFmpegService.Variant> variants, String prefix) {
        Map<String, String> qualities = new LinkedHashMap<>();
        for (FFmpegService.Variant v : variants) {
            qualities.put(v.name(), prefix + v.name() + "/index.m3u8");
        }
        qualities.put("master", prefix + "master.m3u8");
        return qualities;
    }

    private String serializeQualities(Map<String, String> qualities) {
        try {
            return mapper.writeValueAsString(qualities);
        } catch (Exception e) {
            throw new NotFoundException("Failed to serialize qualities to JSON: " + e.getMessage());
        }
    }
}
