package com.pbl6.cinemate.auth_service.mapper;

import com.pbl6.cinemate.auth_service.entity.UserDevice;
import com.pbl6.cinemate.auth_service.payload.response.UserDeviceResponse;

public class UserDeviceMapper {

    private UserDeviceMapper() {
    }

    public static UserDeviceResponse toUserDeviceResponse(UserDevice userDevice) {
        return UserDeviceResponse.builder()
                .id(userDevice.getId())
                .deviceName(userDevice.getDeviceName())
                .deviceType(userDevice.getDeviceType())
                .deviceOs(userDevice.getDeviceOs())
                .browser(userDevice.getBrowser())
                .ipAddress(userDevice.getIpAddress())
                .isCurrent(userDevice.getIsCurrent())
                .lastActiveAt(userDevice.getLastActiveAt())
                .createdAt(userDevice.getCreatedAt())
                .build();
    }
}
