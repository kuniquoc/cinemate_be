package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    List<Device> findByUserIdAndIsActiveTrue(UUID userId);
    
    Optional<Device> findByUserIdAndDeviceId(UUID userId, String deviceId);
    
    @Query("SELECT COUNT(d) FROM Device d WHERE d.userId = :userId AND d.isActive = true")
    long countActiveDevicesByUserId(@Param("userId") UUID userId);
    
    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);
    
    List<Device> findByUserId(UUID userId);
}
