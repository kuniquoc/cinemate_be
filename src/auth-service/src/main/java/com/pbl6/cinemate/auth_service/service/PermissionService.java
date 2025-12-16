package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.payload.request.PermissionRequest;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface PermissionService {
    PermissionResponse createPermission(PermissionRequest request);

    PermissionResponse getPermissionById(UUID id);

    List<PermissionResponse> getAllPermissions();

    PermissionResponse updatePermission(UUID id, PermissionRequest request);

    void deletePermission(UUID id);
}
