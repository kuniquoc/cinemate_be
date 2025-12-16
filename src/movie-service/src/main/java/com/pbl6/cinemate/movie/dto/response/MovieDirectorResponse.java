package com.pbl6.cinemate.movie.dto.response;

import java.util.List;
import java.util.UUID;

public record MovieDirectorResponse(
        UUID movieId,
        List<DirectorResponse> directors) {
}
