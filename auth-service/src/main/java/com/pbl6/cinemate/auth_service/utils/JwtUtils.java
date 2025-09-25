package com.pbl6.cinemate.auth_service.utils;


import com.pbl6.cinemate.auth_service.config.AppProperties;
import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.UserPrincipal;
import com.pbl6.cinemate.auth_service.exception.UnauthenticatedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final AppProperties appProperties;

    public String generateToken(UserPrincipal userPrincipal, boolean isRefreshToken) {
        return Jwts.builder().setSubject(UUID.randomUUID().toString())
                .claim("user_id", userPrincipal.getId())
                .claim("username", userPrincipal.getUsername())
                .claim("role", userPrincipal.getAuthorities().stream()
                        .map(role -> role.getAuthority()).toList())
                .setIssuedAt(new Date())
                .setExpiration(isRefreshToken
                        ? new Date(new Date().getTime() + appProperties.getAuth().getRefreshTokenExpirationMsec())
                        : new Date(new Date().getTime() + appProperties.getAuth().getAccessTokenExpirationMsec()))
                .signWith(isRefreshToken ? getRefreshTokenSecretKey() : getAccessTokenSecretKey(),
                        SignatureAlgorithm.HS512).compact();
    }

    private Key getAccessTokenSecretKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(appProperties.getAuth().getAccessTokenSecret()));
    }

    private Key getRefreshTokenSecretKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(appProperties.getAuth().getRefreshTokenSecret()));
    }


    public Claims verifyToken(String token, boolean isRefreshToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(!isRefreshToken ? getAccessTokenSecretKey() : getRefreshTokenSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new UnauthenticatedException(getExpiredErrorMessage(isRefreshToken));
        } catch (Exception e) {
            throw new UnauthenticatedException(getInvalidErrorMessage(isRefreshToken));
        }
    }

    private String getExpiredErrorMessage(boolean isRefreshToken) {
        return isRefreshToken
                ? ErrorMessage.EXPIRED_REFRESH_TOKEN
                : ErrorMessage.EXPIRED_ACCESS_TOKEN;
    }

    private String getInvalidErrorMessage(boolean isRefreshToken) {
        return isRefreshToken
                ? ErrorMessage.INVALID_REFRESH_TOKEN
                : ErrorMessage.INVALID_ACCESS_TOKEN;
    }

    public Long getTokenAvailableDuration(Claims claims) {
        Date expiration = claims.getExpiration();
        long now = System.currentTimeMillis();

        return expiration.getTime() - now;
    }

    public String getJwtIdFromJWTClaims(Claims claims) {
        return claims.getSubject();
    }

    public String getUsernameFromJWTClaims(Claims claims) {
        return claims.get("username", String.class);
    }

}
