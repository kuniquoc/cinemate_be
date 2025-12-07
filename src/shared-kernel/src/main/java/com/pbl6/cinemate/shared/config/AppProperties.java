package com.pbl6.cinemate.shared.config;

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
    private final Email email = new Email();

    public Auth getAuth() {
        return auth;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Email getEmail() {
        return email;
    }

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

    @Getter
    @Setter
    public static class Email {
        private String from;
        private String fromName;
    }
}