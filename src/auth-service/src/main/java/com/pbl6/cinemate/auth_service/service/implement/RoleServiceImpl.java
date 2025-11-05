package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.constant.ErrorMessage;
import com.pbl6.cinemate.auth_service.entity.Permission;
import com.pbl6.cinemate.auth_service.entity.Role;
import com.pbl6.cinemate.auth_service.exception.BadRequestException;
import com.pbl6.cinemate.auth_service.exception.NotFoundException;
import com.pbl6.cinemate.auth_service.mapper.PermissionMapper;
import com.pbl6.cinemate.auth_service.mapper.RoleMapper;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;
import com.pbl6.cinemate.auth_service.payload.response.RoleResponse;
import com.pbl6.cinemate.auth_service.repository.PermissionRepository;
import com.pbl6.cinemate.auth_service.repository.RoleRepository;
import com.pbl6.cinemate.auth_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Role findByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
    }

    @Override
    public RoleResponse addRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new BadRequestException(ErrorMessage.ROLE_ALREADY_EXISTED);
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        Role saved = roleRepository.save(role);
        return RoleMapper.mapToResponse(saved);
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(RoleMapper::mapToResponse).toList();
    }

    @Override
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
        return RoleMapper.mapToResponse(role);
    }

    @Override
    public RoleResponse updateRole(UUID roleId, String newName, String newDescription) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));
        role.setName(newName);
        role.setDescription(newDescription);
        Role updated = roleRepository.save(role);
        return RoleMapper.mapToResponse(updated);
    }

    // ===== Permission management =====
    @Override
    public void addPermissionsToRole(UUID roleId, List<UUID> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
    }

    @Override
    public void removePermissionFromRole(UUID roleId, UUID permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PERMISSION_NOT_FOUND));

        // Xóa từ phía Role (owner)
        role.removePermission(permission);

        roleRepository.save(role);
    }

    @Override
    public List<PermissionResponse> getPermissionsByRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.ROLE_NOT_FOUND));

        return role.getPermissions().stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionMapper::toResponse)
                .toList();
    }
}
