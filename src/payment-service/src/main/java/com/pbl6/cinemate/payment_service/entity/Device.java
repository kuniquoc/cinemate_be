package com.pbl6.cinemate.payment_service.entity;

import com.pbl6.cinemate.payment_service.enums.DeviceType;
import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "device_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Device extends AbstractBaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "browser_info")
    private String browserInfo;

    @Column(name = "os_info")
    private String osInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (lastAccessed == null) {
            lastAccessed = Instant.now();
        }
    }
}
