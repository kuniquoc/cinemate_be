package com.pbl6.cinemate.movie.service;

import java.nio.file.Path;
import java.util.UUID;

import org.springframework.lang.NonNull;

public interface MovieTranscodeService {
    void transcodeMovie(@NonNull UUID movieId, Path inputFile);
}
