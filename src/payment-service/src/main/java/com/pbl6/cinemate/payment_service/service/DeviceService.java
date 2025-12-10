package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.dto.request.RegisterDeviceRequest;
import com.pbl6.cinemate.payment_service.dto.response.DeviceResponse;
import com.pbl6.cinemate.payment_service.entity.Device;
import com.pbl6.cinemate.payment_service.exception.DeviceLimitException;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final ModelMapper modelMapper;
    
    private static final int MAX_DEVICES = 4;
    
    @Transactional
    public DeviceResponse registerDevice(RegisterDeviceRequest request, UUID userId) {
        // Check if device already exists for this user
        Optional<Device> existingDevice = deviceRepository.findByUserIdAndDeviceId(
                userId, 
                request.getDeviceId()
        );
        
        if (existingDevice.isPresent()) {
            // Update existing device
            Device device = existingDevice.get();
            device.setDeviceName(request.getDeviceName());
            device.setDeviceType(request.getDeviceType());
            device.setBrowserInfo(request.getBrowserInfo());
            device.setOsInfo(request.getOsInfo());
            device.setIpAddress(request.getIpAddress());
            device.setLastAccessed(LocalDateTime.now());
            device.setIsActive(true);
            
            Device updatedDevice = deviceRepository.save(device);
            log.info("Updated existing device: {} for user: {}", device.getId(), userId);
            
            return modelMapper.map(updatedDevice, DeviceResponse.class);
        }
        
        // Check device limit
        verifyDeviceLimit(userId);
        
        // Create new device with server-controlled userId
        Device device = new Device();
        device.setUserId(userId);
        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setDeviceId(request.getDeviceId());
        device.setBrowserInfo(request.getBrowserInfo());
        device.setOsInfo(request.getOsInfo());
        device.setIpAddress(request.getIpAddress());
        device.setIsActive(true);
        
        Device savedDevice = deviceRepository.save(device);
        log.info("Registered new device: {} for user: {}", savedDevice.getId(), userId);
        
        return modelMapper.map(savedDevice, DeviceResponse.class);
    }
    
    @Transactional
    public void removeDevice(UUID deviceId, UUID userId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", "id", deviceId));
        
        if (!device.getUserId().equals(userId)) {
            throw new DeviceLimitException("You can only remove your own devices");
        }
        
        device.setIsActive(false);
        deviceRepository.save(device);
        log.info("Removed device: {} for user: {}", deviceId, userId);
    }
    
    @Transactional(readOnly = true)
    public List<DeviceResponse> getUserDevices(UUID userId) {
        return deviceRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(device -> modelMapper.map(device, DeviceResponse.class))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public void verifyDeviceLimit(UUID userId) {
        long activeDeviceCount = deviceRepository.countActiveDevicesByUserId(userId);
        
        if (activeDeviceCount >= MAX_DEVICES) {
            throw new DeviceLimitException(
                    String.format("Device limit reached. Maximum %d devices allowed. Please remove a device before adding a new one.", 
                            MAX_DEVICES)
            );
        }
    }
    
    @Transactional(readOnly = true)
    public boolean isDeviceRegistered(UUID userId, String deviceId) {
        return deviceRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }
    
    @Transactional
    public void updateDeviceAccess(UUID userId, String deviceId) {
        Optional<Device> device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId);
        device.ifPresent(d -> {
            d.setLastAccessed(LocalDateTime.now());
            deviceRepository.save(d);
        });
    }
}
