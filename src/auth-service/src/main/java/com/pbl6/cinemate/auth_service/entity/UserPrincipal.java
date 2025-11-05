package com.pbl6.cinemate.auth_service.entity;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder
public class UserPrincipal implements UserDetails {
    @Getter
    private UUID id;
    private String email;
    private String password;
    private Role role;
    private Boolean enabled;

    public static UserPrincipal createUserPrincipal(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(user.getRole()) // chỉ 1 role
                .enabled(user.getIsEnabled())
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Bắt buộc có role với prefix ROLE_
        List<GrantedAuthority> authorities =
                new ArrayList<>(List.of(new SimpleGrantedAuthority("ROLE_" + role.getName())));

        // Thêm tất cả permission của role (nếu có)
        if (role.getPermissions() != null) {
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getName())));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
