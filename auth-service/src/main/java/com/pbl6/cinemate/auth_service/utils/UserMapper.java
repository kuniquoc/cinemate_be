package com.pbl6.cinemate.auth_service.utils;

import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.payload.request.SignUpRequest;
import com.pbl6.cinemate.auth_service.payload.response.UserResponse;

public final class UserMapper {
    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isEnabled(user.getIsEnabled())
                .build();
    }

    public static User toUser(SignUpRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
    }

    private UserMapper() {
    }
}
