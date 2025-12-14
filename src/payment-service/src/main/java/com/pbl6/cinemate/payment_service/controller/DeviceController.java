package com.pbl6.cinemate.payment_service.controller;

import com.pbl6.cinemate.payment_service.dto.request.RegisterDeviceRequest;
import com.pbl6.cinemate.payment_service.dto.response.DeviceResponse;
import com.pbl6.cinemate.payment_service.service.DeviceService;
import com.pbl6.cinemate.shared.dto.general.ResponseData;
import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

        private final DeviceService deviceService;

        @GetMapping("/user")
        public ResponseEntity<ResponseData> getUserDevices(
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                List<DeviceResponse> devices = deviceService.getUserDevices(userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                devices,
                                "User devices retrieved successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @PostMapping("/register")
        public ResponseEntity<ResponseData> registerDevice(
                        @Valid @RequestBody RegisterDeviceRequest request,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                DeviceResponse device = deviceService.registerDevice(request, userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                device,
                                "Device registered successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @DeleteMapping("/{deviceId}")
        public ResponseEntity<ResponseData> removeDevice(
                        @PathVariable UUID deviceId,
                        @CurrentUser UserPrincipal userPrincipal,
                        HttpServletRequest httpRequest) {
                deviceService.removeDevice(deviceId, userPrincipal.getId());
                return ResponseEntity.ok(ResponseData.success(
                                "Device removed successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }

        @GetMapping("/verify")
        public ResponseEntity<ResponseData> verifyDevice(
                        @CurrentUser UserPrincipal userPrincipal,
                        @RequestParam(name = "deviceId") String deviceId,
                        HttpServletRequest httpRequest) {
                boolean isRegistered = deviceService.isDeviceRegistered(userPrincipal.getId(), deviceId);

                Map<String, Object> response = new HashMap<>();
                response.put("isRegistered", isRegistered);
                response.put("userId", userPrincipal.getId());
                response.put("deviceId", deviceId);

                return ResponseEntity.ok(ResponseData.success(
                                response,
                                "Device verification completed successfully",
                                httpRequest.getRequestURI(),
                                httpRequest.getMethod()));
        }
}
