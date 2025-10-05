package com.pbl6.cinemate.movie.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pbl6.cinemate.movie.exception.InternalServerException;
import com.pbl6.cinemate.movie.service.FFmpegService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FFmpegServiceImpl implements FFmpegService {
    @Value("${ffmpeg.executable:ffmpeg}")
    private String ffmpegExecutable;

    @Value("${ffmpeg.tmp-dir:/tmp/movies}")
    private String tmpBaseDir;

    public Map<String, Path> transcode(Path inputFile, UUID movieId, List<Variant> variants) {
        Map<String, Path> result = new LinkedHashMap<>();
        Path baseOut = createOutputDirectory(movieId);

        for (Variant v : variants) {
            Path outDir = createVariantDirectory(baseOut, v);
            Path playlist = outDir.resolve("index.m3u8");

            List<String> command = List.of(
                    ffmpegExecutable, "-y", "-i", inputFile.toString(),
                    "-vf", "scale=" + v.resolution(),
                    "-c:a", "aac", "-b:a", v.audioBitrate(),
                    "-c:v", "h264", "-b:v", v.videoBitrate(),
                    "-hls_time", "6", "-hls_playlist_type", "vod",
                    "-hls_segment_filename", outDir.resolve("seg_%03d.ts").toString(),
                    playlist.toString());

            run(command, outDir.toFile());
            result.put(v.name(), outDir);
        }
        createMasterPlaylist(baseOut, variants);
        return result;
    }

    private Path createOutputDirectory(UUID movieId) {
        try {
            Path baseOut = Paths.get(tmpBaseDir, String.valueOf(movieId));
            Files.createDirectories(baseOut);
            return baseOut;
        } catch (Exception e) {
            throw new InternalServerException("Failed to create output directory: " + e.getMessage());
        }
    }

    private Path createVariantDirectory(Path baseOut, Variant variant) {
        try {
            Path outDir = baseOut.resolve(variant.name());
            Files.createDirectories(outDir);
            return outDir;
        } catch (Exception e) {
            throw new InternalServerException("Failed to create variant directory: " + e.getMessage());
        }
    }

    private void run(List<String> command, File dir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Log output if needed
                    log.debug("FFmpeg output: {}", line);
                }
            }
            if (p.waitFor() != 0)
                throw new InternalServerException("FFmpeg process failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("FFmpeg process was interrupted: " + e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException("Failed to run FFmpeg command: " + e.getMessage());
        }
    }

    private void createMasterPlaylist(Path baseOut, List<Variant> variants) {
        try (BufferedWriter bw = Files.newBufferedWriter(baseOut.resolve("master.m3u8"))) {
            bw.write("#EXTM3U%n".formatted());
            for (Variant v : variants) {
                bw.write(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s%n",
                        v.bandwidth(), v.resolution()));
                bw.write(v.name() + "/index.m3u8%n".formatted());
            }
        } catch (IOException e) {
            throw new InternalServerException("Failed to create master playlist: " + e.getMessage());
        }
    }

}
