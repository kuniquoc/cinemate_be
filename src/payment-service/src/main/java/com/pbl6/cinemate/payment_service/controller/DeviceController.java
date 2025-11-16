package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.RegisterDeviceRequest;
import com.pbl6.cinemate.payment_service.dto.response.DeviceResponse;
import com.pbl6.cinemate.payment_service.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    
    private final DeviceService deviceService;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceResponse>> getUserDevices(@PathVariable Long userId) {
        List<DeviceResponse> devices = deviceService.getUserDevices(userId);
        return ResponseEntity.ok(devices);
    }
    
    @PostMapping("/register")
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        DeviceResponse device = deviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }
    
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, String>> removeDevice(
            @PathVariable Long deviceId,
            @RequestParam Long userId) {
        deviceService.removeDevice(deviceId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Device removed successfully");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyDevice(
            @RequestParam Long userId,
            @RequestParam String deviceId) {
        boolean isRegistered = deviceService.isDeviceRegistered(userId, deviceId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isRegistered", isRegistered);
        response.put("userId", userId);
        response.put("deviceId", deviceId);
        
        return ResponseEntity.ok(response);
    }
}
