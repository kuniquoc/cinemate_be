package com.pbl6.microservices.customer_service.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UUID userId;

    public UserPrincipal(UUID userId, String username, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.authorities = authorities;
        this.userId = userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // không cần, vì JWT trust rồi
    }

    @Override
    public String getUsername() {
        return username;
    }

}
