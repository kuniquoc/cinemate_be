package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.payload.request.PermissionRequest;
import com.pbl6.cinemate.auth_service.payload.response.PermissionResponse;
import com.pbl6.cinemate.auth_service.service.PermissionService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPath.PERMISSIONS)
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    // ================== CRUD Permission ==================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseData> createPermission(@RequestBody @Valid PermissionRequest request,
                                                         HttpServletRequest httpServletRequest) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.PERMISSION_CREATED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData> getPermission(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        PermissionResponse response = permissionService.getPermissionById(id);
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.PERMISSION_FETCHED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
    }

    @GetMapping
    public ResponseEntity<ResponseData> getAllPermissions(HttpServletRequest httpServletRequest) {
        List<PermissionResponse> responses = permissionService.getAllPermissions();
        return ResponseEntity.ok(ResponseData.success(responses, FeedbackMessage.PERMISSIONS_FETCHED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData> updatePermission(@PathVariable UUID id,
                                                         @RequestBody @Valid PermissionRequest request,
                                                         HttpServletRequest httpServletRequest) {
        PermissionResponse response = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(ResponseData.success(response, FeedbackMessage.PERMISSION_UPDATED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData> deletePermission(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(ResponseData.successWithoutMetaAndData(FeedbackMessage.PERMISSION_DELETED,
                httpServletRequest.getRequestURI(), httpServletRequest.getMethod()));
    }
}
