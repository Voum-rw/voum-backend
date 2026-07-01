package com.voum.modules.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores per-user notification preferences.
 * Future-ready: the table exists now; UI controls come in a later sprint.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ride_notifications", nullable = false)
    private boolean rideNotifications;

    @Column(name = "system_notifications", nullable = false)
    private boolean systemNotifications;

    @Column(name = "marketing_notifications", nullable = false)
    private boolean marketingNotifications;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
