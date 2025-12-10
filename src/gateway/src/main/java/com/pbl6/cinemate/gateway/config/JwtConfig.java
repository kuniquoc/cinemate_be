package com.pbl6.cinemate.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for JWT validation in API Gateway.
 * Only validates signature and expiry - full authorization is done by backend
 * services.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * Secret key for JWT validation (must match the key used by auth-service)
     */
    private String secret;

    /**
     * List of paths that should skip JWT validation (public endpoints)
     */
    private List<String> whitelistPaths = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getWhitelistPaths() {
        return whitelistPaths;
    }

    public void setWhitelistPaths(List<String> whitelistPaths) {
        this.whitelistPaths = whitelistPaths;
    }
}
