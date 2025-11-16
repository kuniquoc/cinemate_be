package com.pbl6.cinemate.payment_service.dto.response;

import com.pbl6.cinemate.payment_service.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    
    private Long id;
    private Long userId;
    private String deviceName;
    private DeviceType deviceType;
    private String deviceId;
    private String browserInfo;
    private String osInfo;
    private String ipAddress;
    private LocalDateTime lastAccessed;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
