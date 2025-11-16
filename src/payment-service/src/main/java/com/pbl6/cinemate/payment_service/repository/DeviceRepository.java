package com.pbl6.cinemate.payment_service.repository;

import com.pbl6.cinemate.payment_service.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    List<Device> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<Device> findByUserIdAndDeviceId(Long userId, String deviceId);
    
    @Query("SELECT COUNT(d) FROM Device d WHERE d.userId = :userId AND d.isActive = true")
    long countActiveDevicesByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);
    
    List<Device> findByUserId(Long userId);
}
