package com.pbl6.cinemate.movie.dto.response;

import java.util.Map;
import java.util.UUID;

public record MovieStatusResponse(UUID movieId, String status, Map<String, String> qualities) {
}
