package com.pbl6.cinemate.auth_service.mapper;

import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.payload.response.RoleResponse;

public final class RoleMapper {
    private RoleMapper() {
    }

    public static RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
