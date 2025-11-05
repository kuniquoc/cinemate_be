package com.pbl6.cinemate.auth_service.service;

import com.pbl6.cinemate.auth_service.entity.UserDevice;
import com.pbl6.cinemate.auth_service.payload.request.DeviceInfoRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface UserDeviceService {

    UserDevice trackDeviceOnLogin(UUID userId, DeviceInfoRequest deviceInfo, HttpServletRequest request);

    List<UserDevice> getLoggedInDevices(UUID userId);

    void logoutFromDevice(UUID userId, UUID deviceId);

    void logoutFromAllDevices(UUID userId, UUID currentDeviceId);

    UserDevice findById(UUID deviceId);

    // Admin methods
    List<UserDevice> adminGetUserDevices(UUID userId);

    void adminLogoutFromDevice(UUID deviceId);

    void adminLogoutFromAllUserDevices(UUID userId);
}
