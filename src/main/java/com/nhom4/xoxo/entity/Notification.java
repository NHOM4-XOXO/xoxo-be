package com.nhom4.xoxo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Người nhận notification

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "target_id")
    private Long targetId; // ID của object liên quan

    @Column(name = "target_type")
    private String targetType; // Loại object: POST, COMMENT, USER, etc.

    @Column(name = "sender_id")
    private Long senderId; // Người gửi notification (nếu có)

    @Column(name = "action_type")
    private String actionType; // LIKE, COMMENT, FRIEND_REQUEST, etc.

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON data bổ sung
}