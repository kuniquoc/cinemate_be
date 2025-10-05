package com.pbl6.microservices.customer_service.utils;


import com.pbl6.microservices.customer_service.config.AppProperties;
import com.pbl6.microservices.customer_service.constants.ErrorMessage;
import com.pbl6.microservices.customer_service.exception.UnauthenticatedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final AppProperties appProperties;

    private Key getAccessTokenSecretKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(appProperties.getAuth().getAccessTokenSecret()));
    }

    public Claims verifyToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getAccessTokenSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new UnauthenticatedException(ErrorMessage.EXPIRED_ACCESS_TOKEN);
        } catch (Exception e) {
            throw new UnauthenticatedException(ErrorMessage.INVALID_ACCESS_TOKEN);
        }
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
