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

import com.pbl6.cinemate.movie.service.FFmpegService;
import com.pbl6.cinemate.shared.exception.InternalServerException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FFmpegServiceImpl implements FFmpegService {
    @Value("${ffmpeg.executable:ffmpeg}")
    private String ffmpegExecutable;

    @Value("${ffmpeg.tmp-dir:/tmp/movies}")
    private String tmpBaseDir;

    @Override
    public VideoMetadata getVideoMetadata(Path inputFile) {
        try {
            List<String> command = List.of(
                    "ffprobe", "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height,bit_rate,codec_name",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    inputFile.toString());

            List<String> lines = runProcessAndCollect(command);

            String streamLine = lines.size() > 0 ? lines.get(0) : null;
            String formatLine = lines.size() > 1 ? lines.get(1) : null;

            if (streamLine == null || streamLine.isEmpty()) {
                throw new InternalServerException("Failed to parse video metadata");
            }

            VideoInfo vInfo = parseStreamLine(streamLine);
            long durationSeconds = parseDurationLine(formatLine);
            String audioCodec = probeAudioCodec(inputFile);

            log.info("Video metadata: {}x{}, bitrate: {}, duration: {}, vcodec: {}, acodec:{}",
                    vInfo.width, vInfo.height, vInfo.bitrate, durationSeconds, vInfo.codec, audioCodec);

            return new VideoMetadata(vInfo.width, vInfo.height, vInfo.bitrate, durationSeconds, vInfo.codec,
                    audioCodec);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("ffprobe process was interrupted: " + e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException("Failed to get video metadata: " + e.getMessage());
        }
    }

    private record VideoInfo(int width, int height, long bitrate, String codec) {
    }

    private VideoInfo parseStreamLine(String streamLine) {
        String[] parts = streamLine.split(",");
        int width = 0;
        int height = 0;
        long bitrate = 0L;
        String codec = null;

        if (parts.length > 0) {
            try {
                width = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                log.debug("Unable to parse width from stream line ('{}'): {}", streamLine, e.getMessage());
            }
        }
        if (parts.length > 1) {
            try {
                height = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                log.debug("Unable to parse height from stream line ('{}'): {}", streamLine, e.getMessage());
            }
        }

        // Remaining parts may contain bitrate and codec, but ordering/availability can
        // vary.
        for (int i = 2; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.isEmpty() || "N/A".equals(p))
                continue;
            // Try bitrate first (numeric). If not numeric, treat as codec.
            if (codec == null) {
                try {
                    bitrate = Long.parseLong(p);
                    continue;
                } catch (NumberFormatException ex) {
                    // not a number -> likely codec
                    codec = p;
                }
            } else {
                // codec already set, ignore extra fields
            }
        }

        return new VideoInfo(width, height, bitrate, codec);
    }

    private long parseDurationLine(String formatLine) {
        if (formatLine == null || formatLine.isEmpty())
            return 0L;
        try {
            double d = Double.parseDouble(formatLine.trim());
            return Math.round(d);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String probeAudioCodec(Path inputFile) {
        try {
            List<String> acmd = List.of(
                    "ffprobe", "-v", "error",
                    "-select_streams", "a:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "csv=p=0",
                    inputFile.toString());
            List<String> out = runProcessAndCollect(acmd);
            return out.size() > 0 ? out.get(0).trim() : null;
        } catch (Exception ex) {
            log.debug("Failed to get audio codec: {}", ex.getMessage());
            return null;
        }
    }

    private List<String> runProcessAndCollect(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            return br.lines().toList();
        } finally {
            p.waitFor();
        }
    }

    @Override
    public Map<String, Path> transcode(Path inputFile, UUID movieId, List<Variant> variants, VideoMetadata metadata) {
        Map<String, Path> result = new LinkedHashMap<>();
        Path baseOut = createOutputDirectory(movieId);

        for (Variant v : variants) {
            Path outDir = createVariantDirectory(baseOut, v);
            Path playlist = outDir.resolve("playlist.m3u8");

            // Use fMP4 format instead of MPEG-TS
            List<String> command = List.of(
                    ffmpegExecutable, "-y", "-i", inputFile.toString(),
                    "-vf", "scale=" + v.resolution(),
                    "-c:a", "aac", "-b:a", v.audioBitrate(),
                    "-c:v", "h264", "-b:v", v.videoBitrate(),
                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_type", "fmp4",
                    "-hls_fmp4_init_filename", "init.mp4",
                    "-hls_segment_filename", outDir.resolve("seg_%04d.m4s").toString(),
                    playlist.toString());

            run(command, outDir.toFile());
            result.put(v.name(), outDir);
        }
        createMasterPlaylist(baseOut, variants, metadata);
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

    private void createMasterPlaylist(Path baseOut, List<Variant> variants, VideoMetadata metadata) {
        try (BufferedWriter bw = Files.newBufferedWriter(baseOut.resolve("master.m3u8"))) {
            bw.write("#EXTM3U%n".formatted());
            String codecsAttr = buildCodecsAttr(metadata);
            if (metadata != null && metadata.durationSeconds() > 0) {
                bw.write(String.format("#EXT-X-TOTALDURATION:%d%n", metadata.durationSeconds()));
            }
            for (Variant v : variants) {
                writeStreamInf(bw, v, codecsAttr);
                bw.write(v.name() + "/playlist.m3u8%n".formatted());
            }
        } catch (IOException e) {
            throw new InternalServerException("Failed to create master playlist: " + e.getMessage());
        }
    }

    private String buildCodecsAttr(VideoMetadata metadata) {
        if (metadata == null)
            return null;
        String v = metadata.videoCodec();
        String a = metadata.audioCodec();
        if ((v == null || v.isBlank()) && (a == null || a.isBlank()))
            return null;
        String combined = String.join(",", v != null ? v : "", a != null ? a : "");
        return combined.replaceAll("(^,|,$)", "");
    }

    private void writeStreamInf(BufferedWriter bw, Variant v, String codecsAttr) throws IOException {
        if (codecsAttr != null && !codecsAttr.isEmpty()) {
            bw.write(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s,CODECS=\"%s\"%n",
                    v.bandwidth(), v.resolution(), codecsAttr));
        } else {
            bw.write(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s%n",
                    v.bandwidth(), v.resolution()));
        }
    }

}
