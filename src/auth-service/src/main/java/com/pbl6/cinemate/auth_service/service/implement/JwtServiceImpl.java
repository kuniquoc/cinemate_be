package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.enums.CachePrefix;
import com.pbl6.cinemate.auth_service.payload.response.RefreshTokenResponse;
import com.pbl6.cinemate.auth_service.service.CacheService;
import com.pbl6.cinemate.auth_service.service.JwtService;
import com.pbl6.cinemate.shared.constants.ErrorMessage;
import com.pbl6.cinemate.shared.exception.UnauthenticatedException;
import com.pbl6.cinemate.shared.utils.JwtUtils;

import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtServiceImpl implements JwtService {
    CacheService cacheService;
    JwtUtils jwtUtils;

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        boolean isRefreshToken = true;
        Claims claims = jwtUtils.verifyToken(refreshToken, isRefreshToken);

        if (cacheService.hasKey(CachePrefix.BLACK_LIST_TOKENS.getPrefix() + jwtUtils.getJwtIdFromJWTClaims(claims))) {
            throw new UnauthenticatedException(ErrorMessage.EXPIRED_REFRESH_TOKEN);
        }

        return new RefreshTokenResponse(jwtUtils.refreshToken(claims));
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Claims claims = jwtUtils.verifyToken(token, false);
            return !cacheService.hasKey(CachePrefix.BLACK_LIST_TOKENS.getPrefix()
                    + jwtUtils.getJwtIdFromJWTClaims(claims));
        } catch (Exception e) {
            return false;
        }
    }
}
