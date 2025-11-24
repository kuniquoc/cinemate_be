package com.pbl6.cinemate.shared.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.pbl6.cinemate.shared.config.AppProperties;
import com.pbl6.cinemate.shared.exception.UnauthenticatedException;
import com.pbl6.cinemate.shared.security.UserPrincipal;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final AppProperties appProperties;

    public String generateToken(String userId, String username, String role, List<String> permissions, 
                                    boolean isRefreshToken) {
        return Jwts.builder().setSubject(UUID.randomUUID().toString())
                .claim("user_id", userId)
                .claim("username", username)
                .claim("role", role)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(isRefreshToken
                        ? new Date(new Date().getTime() + appProperties.getAuth().getRefreshTokenExpirationMsec())
                        : new Date(new Date().getTime() + appProperties.getAuth().getAccessTokenExpirationMsec()))
                .signWith(isRefreshToken ? getRefreshTokenSecretKey() : getAccessTokenSecretKey(),
                        SignatureAlgorithm.HS512).compact();
    }

    public String generateToken(UserPrincipal userPrincipal, boolean isRefreshToken) {
        return Jwts.builder().setSubject(UUID.randomUUID().toString())
                .claim("user_id", userPrincipal.getId())
                .claim("username", userPrincipal.getUsername())
                .claim("role", userPrincipal.getAuthorities().stream()
                        .findFirst()
                        .map(role -> "ROLE_" + role.getAuthority())
                        .orElse(null))
                .claim("permissions", userPrincipal.getAuthorities().stream()
                        .skip(1)
                        .map(role -> role.getAuthority()).toList())
                .setIssuedAt(new Date())
                .setExpiration(isRefreshToken
                        ? new Date(new Date().getTime() + appProperties.getAuth().getRefreshTokenExpirationMsec())
                        : new Date(new Date().getTime() + appProperties.getAuth().getAccessTokenExpirationMsec()))
                .signWith(isRefreshToken ? getRefreshTokenSecretKey() : getAccessTokenSecretKey(),
                        SignatureAlgorithm.HS512).compact();
    }

    public String refreshToken(Claims claims) {
        return Jwts.builder().setSubject(UUID.randomUUID().toString())
                .claim("user_id", claims.get("user_id", String.class))
                .claim("username", claims.get("username", String.class))
                .claim("role", claims.get("role", String.class))
                .claim("permissions", claims.get("permissions", List.class))
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + appProperties.getAuth().getAccessTokenExpirationMsec()))
                .signWith(getAccessTokenSecretKey(),
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
                ? "Expired refresh token"
                : "Expired access token";
    }

    private String getInvalidErrorMessage(boolean isRefreshToken) {
        return isRefreshToken
                ? "Invalid refresh token"
                : "Invalid access token";
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
