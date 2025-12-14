package com.pbl6.cinemate.auth_service.mapper;

import com.pbl6.cinemate.auth_service.entity.Permission;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;

public final class PermissionMapper {
    private PermissionMapper() {
    }

    public static PermissionResponse toResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}
