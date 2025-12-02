package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.entity.Permission;
import com.pbl6.cinemate.auth_service.mapper.PermissionMapper;
import com.pbl6.cinemate.auth_service.payload.request.PermissionRequest;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;
import com.pbl6.cinemate.auth_service.repository.PermissionRepository;
import com.pbl6.cinemate.auth_service.service.PermissionService;
import com.pbl6.cinemate.shared.constants.ErrorMessage;
import com.pbl6.cinemate.shared.exception.BadRequestException;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new BadRequestException(ErrorMessage.PERMISSION_NAME_EXISTED);
        }

        Permission permission = new Permission();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());

        Permission saved = permissionRepository.save(permission);
        return PermissionMapper.toResponse(saved);
    }

    @Override
    public PermissionResponse getPermissionById(UUID id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PERMISSION_NOT_FOUND));
        return PermissionMapper.toResponse(permission);
    }

    @Override
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .filter(p -> p.getDeletedAt() == null)
                .map(PermissionMapper::toResponse)
                .toList();
    }

    @Override
    public PermissionResponse updatePermission(UUID id, PermissionRequest request) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PERMISSION_NOT_FOUND));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());

        Permission updated = permissionRepository.save(existing);
        return PermissionMapper.toResponse(updated);
    }

    @Override
    public void deletePermission(UUID id) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.PERMISSION_NOT_FOUND));
        existing.setDeletedAt(LocalDateTime.now());
        permissionRepository.save(existing);
    }
}
