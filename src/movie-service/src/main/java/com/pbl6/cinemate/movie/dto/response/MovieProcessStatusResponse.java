package com.pbl6.cinemate.movie.dto.response;

import java.util.UUID;

public record MovieProcessStatusResponse(UUID movieId, String processStatus) {
}
