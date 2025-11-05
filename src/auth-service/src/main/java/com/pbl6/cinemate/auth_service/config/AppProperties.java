package com.pbl6.cinemate.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Auth auth = new Auth();
    private final Admin admin = new Admin();

    @Getter
    @Setter
    public static class Auth {
        private String accessTokenSecret;
        private long accessTokenExpirationMsec;
        private String refreshTokenSecret;
        private long refreshTokenExpirationMsec;
    }

    @Getter
    @Setter
    public static class Admin {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
    }
}