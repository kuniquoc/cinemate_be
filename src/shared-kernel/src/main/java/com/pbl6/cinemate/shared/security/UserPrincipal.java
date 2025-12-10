package com.pbl6.cinemate.shared.security;

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
    @Getter
    private String role;
    private List<String> permissions;
    private Boolean enabled;
    @Getter
    private String firstName;
    @Getter
    private String lastName;

    public static UserPrincipal createUserPrincipal(String userId, String username,
            String password, String role, List<String> permissions) {
        return createUserPrincipal(userId, username, password, role, permissions, null, null);
    }

    public static UserPrincipal createUserPrincipal(String userId, String username,
            String password, String role, List<String> permissions, String firstName, String lastName) {
        return UserPrincipal.builder()
                .id(UUID.fromString(userId))
                .email(username)
                .password(password)
                .role(role)
                .permissions(permissions)
                .enabled(true)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    public String getFullName() {
        if (firstName == null && lastName == null)
            return null;
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Bắt buộc có role với prefix ROLE_
        List<GrantedAuthority> authorities = new ArrayList<>(List.of(new SimpleGrantedAuthority(role)));

        // Thêm tất cả permission của role (nếu có)
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
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
