package com.pbl6.cinemate.auth_service.controller;

import com.pbl6.cinemate.auth_service.constant.ApiPath;
import com.pbl6.cinemate.auth_service.constant.FeedbackMessage;
import com.pbl6.cinemate.auth_service.entity.UserDevice;
import com.pbl6.cinemate.auth_service.mapper.UserDeviceMapper;
import com.pbl6.cinemate.auth_service.payload.response.UserDeviceResponse;
import com.pbl6.cinemate.auth_service.service.UserDeviceService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPath.DEVICES)
@RequiredArgsConstructor
public class DeviceController {

    private final UserDeviceService userDeviceService;

    @GetMapping
    public ResponseEntity<ResponseData> getLoggedInDevices(@CurrentUser UserPrincipal userPrincipal,
                                                           HttpServletRequest request) {
        List<UserDevice> devices = userDeviceService.getLoggedInDevices(userPrincipal.getId());
        List<UserDeviceResponse> deviceResponses = devices.stream()
                .map(UserDeviceMapper::toUserDeviceResponse)
                .collect(Collectors.toList());
        
        ResponseData responseData = ResponseData.success(deviceResponses,
                FeedbackMessage.DEVICES_FETCHED, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ResponseData> logoutFromDevice(@CurrentUser UserPrincipal userPrincipal,
                                                         @PathVariable UUID deviceId,
                                                         HttpServletRequest request) {
        userDeviceService.logoutFromDevice(userPrincipal.getId(), deviceId);
        
        ResponseData responseData = ResponseData.successWithoutMetaAndData(
                FeedbackMessage.DEVICE_LOGGED_OUT, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping("/logout-all")
    public ResponseEntity<ResponseData> logoutFromAllDevices(@CurrentUser UserPrincipal userPrincipal,
                                                             @RequestParam(required = false) UUID currentDeviceId,
                                                             HttpServletRequest request) {
        userDeviceService.logoutFromAllDevices(userPrincipal.getId(), currentDeviceId);
        
        ResponseData responseData = ResponseData.successWithoutMetaAndData(
                FeedbackMessage.ALL_DEVICES_LOGGED_OUT, request.getRequestURI(), request.getMethod());
        return ResponseEntity.ok(responseData);
    }
}
