package com.nhom4.xoxo.dto.req;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushNotificationRequest {
    
    private String title;
    private String body;
    private String imageUrl;
    private String clickAction;
    private String sound = "default";
    private String priority = "high";
    private Long timeToLive = 86400L; // 24 hours in seconds
    
    // Custom data for the app
    private Map<String, String> data;
    
    // Notification specific data
    private NotificationData notificationData;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class NotificationData {
        private String type; // CHAT, SYSTEM, FRIEND_REQUEST, etc.
        private Long chatRoomId;
        private Long senderId;
        private String senderName;
        private String senderAvatar;
        private String messagePreview;
        private Long messageId;
        private String chatRoomName;
        private String chatRoomType;
    }
}
