package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.payload.general.ResponseData;
import com.pbl6.cinemate.auth_service.payload.request.AddingPermissionRequest;
import com.pbl6.cinemate.auth_service.payload.request.RoleRequest;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;
import com.pbl6.cinemate.auth_service.payload.response.RoleResponse;
import com.pbl6.cinemate.auth_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.ROLES)
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // Thêm role mới
    @PostMapping
    public ResponseEntity<ResponseData> addRole(
            @RequestBody @Valid RoleRequest request) {
        RoleResponse response = roleService.addRole(request.getName(), request.getDescription());
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ROLE_CREATED));
    }

    // Lấy tất cả roles
    @GetMapping
    public ResponseEntity<ResponseData> getAllRoles() {
        List<RoleResponse> responses = roleService.getAllRoles();
        return ResponseEntity.ok(ResponseData.success(responses, FeedbackMessage.ROLES_RETRIEVED));
    }

    // Lấy role theo id
    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getRoleById(@PathVariable UUID id) {
        RoleResponse response = roleService.getRoleById(id);
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ROLE_RETRIEVED));
    }

    // Cập nhật role
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData> updateRole(
            @PathVariable UUID id,
            @RequestBody @Valid RoleRequest request) {
        RoleResponse response = roleService.updateRole(id, request.getName(), request.getDescription());
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.ROLE_UPDATED));
    }

    // ================== Nested Permissions ==================

    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<ResponseData> addPermissionsToRole(
            @PathVariable UUID roleId,
            @RequestBody @Valid AddingPermissionRequest request) {
        roleService.addPermissionsToRole(roleId, request.getPermissionIds());
        return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                FeedbackMessage.PERMISSIONS_ADDED_TO_ROLE));
    }

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<ResponseData> getPermissionsByRole(@PathVariable UUID roleId) {
        List<PermissionResponse> responses = roleService.getPermissionsByRole(roleId);
        return ResponseEntity.ok(ResponseData.success(responses,
                FeedbackMessage.PERMISSIONS_FETCHED_FOR_ROLE));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ResponseData> removePermissionFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId) {
        roleService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(
                FeedbackMessage.PERMISSION_REMOVED_FROM_ROLE));
    }
}
