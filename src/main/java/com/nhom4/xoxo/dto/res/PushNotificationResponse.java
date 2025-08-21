package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushNotificationResponse {
    
    private Long id;
    private Long userId;
    private String fcmToken;
    private String title;
    private String body;
    private String status; // SENT, DELIVERED, FAILED
    private String messageId; // FCM message ID
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private String deviceType; // ANDROID, IOS, WEB
    private String appVersion;
}
