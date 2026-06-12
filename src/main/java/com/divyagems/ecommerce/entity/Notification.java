package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.*;

/**
 * In-app notification for users.
 * redirectUrl is a frontend route that the notification links to
 * (e.g. /orders/DG-2024-00001 for an order update).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_is_read", columnList = "is_read")
})
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationTypeEnum type;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "redirect_url", length = 500)
    private String redirectUrl;
}
