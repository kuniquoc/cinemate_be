package com.pbl6.cinemate.movie.service;

import java.nio.file.Path;
import java.util.UUID;

public interface MovieTranscodeService {
    void transcodeMovie(UUID movieId, Path inputFile);
}
