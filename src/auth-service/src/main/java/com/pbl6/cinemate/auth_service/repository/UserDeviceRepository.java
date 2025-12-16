package com.pbl6.cinemate.auth_service.repository;

import com.pbl6.cinemate.auth_service.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id = :userId AND ud.deletedAt IS NULL ORDER BY ud.lastActiveAt DESC")
    List<UserDevice> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.id = :deviceId AND ud.user.id = :userId AND ud.deletedAt IS NULL")
    Optional<UserDevice> findByIdAndUserId(@Param("deviceId") UUID deviceId, @Param("userId") UUID userId);

    @Query("SELECT ud FROM UserDevice ud WHERE ud.user.id = :userId AND ud.deviceName = :deviceName " +
            "AND ud.deviceOs = :deviceOs AND ud.browser = :browser AND ud.deletedAt IS NULL")
    Optional<UserDevice> findByUserIdAndDeviceInfo(@Param("userId") UUID userId,
                                                   @Param("deviceName") String deviceName,
                                                   @Param("deviceOs") String deviceOs,
                                                   @Param("browser") String browser);

    @Modifying
    @Query("UPDATE UserDevice ud SET ud.isCurrent = false WHERE ud.user.id = :userId")
    void clearCurrentDeviceStatus(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ud) FROM UserDevice ud WHERE ud.user.id = :userId AND ud.deletedAt IS NULL")
    long countActiveDevicesByUserId(@Param("userId") UUID userId);
}
