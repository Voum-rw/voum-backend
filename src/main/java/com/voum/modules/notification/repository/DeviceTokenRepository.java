package com.voum.modules.notification.repository;

import com.voum.modules.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<DeviceToken> findByUserIdAndDeviceToken(UUID userId, String deviceToken);

    Optional<DeviceToken> findByDeviceToken(String deviceToken);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false WHERE dt.deviceToken = :token")
    void deactivateByToken(@Param("token") String token);
}
