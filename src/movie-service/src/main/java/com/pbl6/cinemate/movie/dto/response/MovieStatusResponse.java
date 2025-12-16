package com.pbl6.cinemate.movie.dto.response;

import java.util.List;
import java.util.UUID;

public record MovieStatusResponse(UUID movieId, String status, List<String> qualities) {
}
