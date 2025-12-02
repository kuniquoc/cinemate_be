package com.pbl6.cinemate.payment_service.dto.request;

import java.util.UUID;

import com.pbl6.cinemate.payment_service.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotBlank(message = "Device name is required")
    private String deviceName;
    
    @NotNull(message = "Device type is required")
    private DeviceType deviceType;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    private String browserInfo;
    
    private String osInfo;
    
    private String ipAddress;
}
