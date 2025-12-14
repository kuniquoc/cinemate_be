package com.pbl6.cinemate.auth_service.service.implement;

import com.pbl6.cinemate.auth_service.entity.User;
import com.pbl6.cinemate.auth_service.entity.UserDevice;
import com.pbl6.cinemate.auth_service.payload.request.DeviceInfoRequest;
import com.pbl6.cinemate.auth_service.repository.UserDeviceRepository;
import com.pbl6.cinemate.auth_service.service.UserDeviceService;
import com.pbl6.cinemate.auth_service.service.UserService;
import com.pbl6.cinemate.shared.constants.ErrorMessage;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserService userService;

    @Override
    @Transactional
    public UserDevice trackDeviceOnLogin(UUID userId, DeviceInfoRequest deviceInfo, HttpServletRequest request) {
        User user = userService.findById(userId);
        String ipAddress = extractIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        String deviceName = deviceInfo != null && deviceInfo.getDeviceName() != null
                ? deviceInfo.getDeviceName()
                : extractDeviceNameFromUserAgent(userAgent);
        String deviceType = deviceInfo != null ? deviceInfo.getDeviceType() : extractDeviceType(userAgent);
        String deviceOs = deviceInfo != null && deviceInfo.getDeviceOs() != null
                ? deviceInfo.getDeviceOs()
                : extractOS(userAgent);
        String browser = deviceInfo != null && deviceInfo.getBrowser() != null
                ? deviceInfo.getBrowser()
                : extractBrowser(userAgent);

        // Check if device already exists
        Optional<UserDevice> existingDevice = userDeviceRepository.findByUserIdAndDeviceInfo(
                userId, deviceName, deviceOs, browser);

        UserDevice device;
        if (existingDevice.isPresent()) {
            // Update existing device
            device = existingDevice.get();
            device.setIpAddress(ipAddress);
            device.setUserAgent(userAgent);
            device.setLastActiveAt(Instant.now());
            device.setDeviceType(deviceType);
        } else {
            // Create new device
            device = UserDevice.builder()
                    .user(user)
                    .deviceName(deviceName)
                    .deviceType(deviceType)
                    .deviceOs(deviceOs)
                    .browser(browser)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .lastActiveAt(Instant.now())
                    .build();
        }

        // Clear current device status for all devices
        userDeviceRepository.clearCurrentDeviceStatus(userId);

        // Set this device as current
        device.setIsCurrent(true);

        return userDeviceRepository.save(device);
    }

    @Override
    public List<UserDevice> getLoggedInDevices(UUID userId) {
        return userDeviceRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public void logoutFromDevice(UUID userId, UUID deviceId) {
        UserDevice device = userDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.DEVICE_NOT_FOUND));

        device.setDeletedAt(Instant.now());
        device.setIsCurrent(false);
        userDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void logoutFromAllDevices(UUID userId, UUID currentDeviceId) {
        List<UserDevice> devices = userDeviceRepository.findAllByUserId(userId);

        for (UserDevice device : devices) {
            if (!device.getId().equals(currentDeviceId)) {
                device.setDeletedAt(Instant.now());
                device.setIsCurrent(false);
                userDeviceRepository.save(device);
            }
        }
    }

    @Override
    public UserDevice findById(UUID deviceId) {
        return userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.DEVICE_NOT_FOUND));
    }

    @Override
    public List<UserDevice> adminGetUserDevices(UUID userId) {
        // Verify user exists
        userService.findById(userId);
        return userDeviceRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public void adminLogoutFromDevice(UUID deviceId) {
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.DEVICE_NOT_FOUND));

        device.setDeletedAt(Instant.now());
        device.setIsCurrent(false);
        userDeviceRepository.save(device);
    }

    @Override
    @Transactional
    public void adminLogoutFromAllUserDevices(UUID userId) {
        // Verify user exists
        userService.findById(userId);

        List<UserDevice> devices = userDeviceRepository.findAllByUserId(userId);
        for (UserDevice device : devices) {
            device.setDeletedAt(Instant.now());
            device.setIsCurrent(false);
            userDeviceRepository.save(device);
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        // In case of multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }

    private String extractDeviceNameFromUserAgent(String userAgent) {
        if (userAgent == null)
            return "Unknown Device";

        if (userAgent.contains("Windows"))
            return "Windows PC";
        if (userAgent.contains("Macintosh"))
            return "Mac";
        if (userAgent.contains("Linux"))
            return "Linux PC";
        if (userAgent.contains("iPhone"))
            return "iPhone";
        if (userAgent.contains("iPad"))
            return "iPad";
        if (userAgent.contains("Android"))
            return "Android Device";

        return "Unknown Device";
    }

    private String extractDeviceType(String userAgent) {
        if (userAgent == null)
            return "Unknown";

        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            return "Mobile";
        }
        if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String extractOS(String userAgent) {
        if (userAgent == null)
            return "Unknown";

        if (userAgent.contains("Windows NT 10.0"))
            return "Windows 10";
        if (userAgent.contains("Windows NT 11.0"))
            return "Windows 11";
        if (userAgent.contains("Windows NT 6.3"))
            return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2"))
            return "Windows 8";
        if (userAgent.contains("Windows NT 6.1"))
            return "Windows 7";
        if (userAgent.contains("Mac OS X")) {
            int startIndex = userAgent.indexOf("Mac OS X");
            if (startIndex != -1) {
                String osVersion = userAgent.substring(startIndex + 9, Math.min(startIndex + 20, userAgent.length()));
                return "macOS " + osVersion.split("[);]")[0].trim().replace("_", ".");
            }
            return "macOS";
        }
        if (userAgent.contains("Linux"))
            return "Linux";
        if (userAgent.contains("Android")) {
            int startIndex = userAgent.indexOf("Android");
            if (startIndex != -1) {
                String osVersion = userAgent.substring(startIndex + 8, Math.min(startIndex + 15, userAgent.length()));
                return "Android " + osVersion.split("[;)]")[0].trim();
            }
            return "Android";
        }
        if (userAgent.contains("iPhone OS")) {
            int startIndex = userAgent.indexOf("iPhone OS");
            if (startIndex != -1) {
                String osVersion = userAgent.substring(startIndex + 10, Math.min(startIndex + 17, userAgent.length()));
                return "iOS " + osVersion.split("[_)]")[0].trim();
            }
            return "iOS";
        }
        if (userAgent.contains("iPad"))
            return "iPadOS";

        return "Unknown";
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null)
            return "Unknown";

        if (userAgent.contains("Edg/"))
            return "Microsoft Edge";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg"))
            return "Google Chrome";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome"))
            return "Safari";
        if (userAgent.contains("Firefox/"))
            return "Firefox";
        if (userAgent.contains("Opera/") || userAgent.contains("OPR/"))
            return "Opera";

        return "Unknown";
    }
}
