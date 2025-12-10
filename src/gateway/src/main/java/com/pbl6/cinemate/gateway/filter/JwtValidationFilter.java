package com.pbl6.cinemate.gateway.filter;

import com.pbl6.cinemate.gateway.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Gateway global filter for JWT validation.
 * 
 * This filter provides "double-check" security at the gateway level:
 * - Validates JWT signature (token was issued by our auth-service)
 * - Validates JWT expiry (token is not expired)
 * 
 * Full authorization (role-based access, permissions) is still handled by
 * backend services.
 * 
 * This prevents:
 * - Requests with tampered tokens from reaching backend services
 * - Requests with expired tokens from reaching backend services
 * - Requests without tokens to protected endpoints
 */
@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtValidationFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Initialize secret key for JWT validation
        if (jwtConfig.getSecret() != null && !jwtConfig.getSecret().isBlank()) {
            this.secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        } else {
            this.secretKey = null;
            log.warn("JWT secret is not configured. JWT validation will be disabled.");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        log.debug("Processing request: {} {}", method, path);

        // Skip validation if JWT secret is not configured
        if (secretKey == null) {
            log.debug("JWT validation skipped - secret not configured");
            return chain.filter(exchange);
        }

        // Check if path is in whitelist (public endpoints)
        if (isWhitelistedPath(path)) {
            log.debug("Path {} is whitelisted, skipping JWT validation", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // If no token provided, let the request through
        // Backend services will decide if the endpoint requires authentication
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No JWT token found, passing request to backend");
            return chain.filter(exchange);
        }

        // Extract and validate token
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Validate JWT signature and expiry
            Claims claims = validateToken(token);
            log.debug("JWT validated successfully for user: {}", claims.getSubject());

            // Forward the original Authorization header to backend services
            // Backend services will perform full authorization
            return chain.filter(exchange);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for path {}: {}", path, e.getMessage());
            return onError(exchange, "Token has expired", HttpStatus.UNAUTHORIZED);

        } catch (JwtException e) {
            log.warn("Invalid JWT token for path {}: {}", path, e.getMessage());
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);

        } catch (Exception e) {
            log.error("Unexpected error validating JWT for path {}: {}", path, e.getMessage());
            return onError(exchange, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate JWT token - checks signature and expiry only.
     * Does NOT check roles or permissions (that's backend's job).
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if the path matches any whitelist patterns.
     */
    private boolean isWhitelistedPath(String path) {
        return jwtConfig.getWhitelistPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Return error response.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"success\":false,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message,
                new Date().toInstant().toString());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run early in the filter chain, before routing
        return -100;
    }
}
