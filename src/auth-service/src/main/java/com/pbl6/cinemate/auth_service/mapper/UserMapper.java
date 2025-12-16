package com.pbl6.cinemate.auth_service.mapper;

import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.payload.request.SignUpRequest;
import com.pbl6.cinemate.auth_service.payload.response.UserResponse;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isEnabled(user.getIsEnabled())
                .role(user.getRole() != null ? RoleMapper.mapToResponse(user.getRole()) : null)
                .accountVerifiedAt(user.getAccountVerifiedAt())
                .build();
    }

    public static User toUser(SignUpRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }
}
