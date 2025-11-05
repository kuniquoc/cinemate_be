package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;
import com.pbl6.cinemate.auth_service.payload.response.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    Role findByName(String name);

    RoleResponse addRole(String name, String description);

    List<RoleResponse> getAllRoles();

    RoleResponse getRoleById(UUID roleId);

    RoleResponse updateRole(UUID roleId, String newName, String newDescription);

    // ===== Permission management =====
    void addPermissionsToRole(UUID roleId, List<UUID> permissionIds);

    void removePermissionFromRole(UUID roleId, UUID permissionId);

    List<PermissionResponse> getPermissionsByRole(UUID roleId);
}
