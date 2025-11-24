package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.entity.UserDevice;
import com.pbl6.cinemate.auth_service.mapper.UserDeviceMapper;
import com.pbl6.cinemate.auth_service.payload.response.UserDeviceResponse;
import com.pbl6.cinemate.auth_service.service.UserDeviceService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPath.ADMIN_DEVICES)
@RequiredArgsConstructor
public class AdminDeviceController {

    private final UserDeviceService userDeviceService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ResponseData> getUserDevices(@PathVariable UUID userId,
                                                        HttpServletRequest request) {
        List<UserDevice> devices = userDeviceService.adminGetUserDevices(userId);
        List<UserDeviceResponse> deviceResponses = devices.stream()
                .map(UserDeviceMapper::toUserDeviceResponse)
                .collect(Collectors.toList());
        
        ResponseData responseData = ResponseData.success(deviceResponses,
                FeedbackMessage.USER_DEVICES_FETCHED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ResponseData> logoutFromDevice(@PathVariable UUID deviceId,
                                                         HttpServletRequest request) {
        userDeviceService.adminLogoutFromDevice(deviceId);
        
        ResponseData responseData = ResponseData.successWithoutMetaAndData(
                FeedbackMessage.USER_DEVICE_LOGGED_OUT, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}/logout-all")
    public ResponseEntity<ResponseData> logoutFromAllUserDevices(@PathVariable UUID userId,
                                                                  HttpServletRequest request) {
        userDeviceService.adminLogoutFromAllUserDevices(userId);
        
        ResponseData responseData = ResponseData.successWithoutMetaAndData(
                FeedbackMessage.ALL_USER_DEVICES_LOGGED_OUT, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }
}
