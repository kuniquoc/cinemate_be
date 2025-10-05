package com.pbl6.cinemate.movie.dto.response;

import java.util.UUID;

public record MovieUploadResponse(UUID movieId, String status) {
}
