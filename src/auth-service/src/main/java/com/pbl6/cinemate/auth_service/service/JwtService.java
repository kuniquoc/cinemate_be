package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.payload.response.RefreshTokenResponse;

public interface JwtService {
    RefreshTokenResponse refreshToken(String refreshToken);

    boolean isTokenValid(String token);
}
