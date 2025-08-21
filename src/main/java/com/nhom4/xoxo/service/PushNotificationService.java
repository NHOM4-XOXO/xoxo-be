package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.req.PushNotificationRequest;
import com.nhom4.xoxo.dto.res.PushNotificationResponse;

import java.util.List;

public interface PushNotificationService {
    
    /**
     * Send push notification to specific user
     */
    PushNotificationResponse sendToUser(Long userId, PushNotificationRequest request);
    
    /**
     * Send push notification to multiple users
     */
    List<PushNotificationResponse> sendToUsers(List<Long> userIds, PushNotificationRequest request);
    
    /**
     * Send push notification to all users in a chat room
     */
    List<PushNotificationResponse> sendToChatRoom(Long chatRoomId, PushNotificationRequest request);
    
    /**
     * Send push notification to topic (for broadcast messages)
     */
    PushNotificationResponse sendToTopic(String topic, PushNotificationRequest request);
    
    /**
     * Subscribe user to a topic
     */
    void subscribeUserToTopic(Long userId, String topic);
    
    /**
     * Unsubscribe user from a topic
     */
    void unsubscribeUserFromTopic(Long userId, String topic);
    
    /**
     * Update user's FCM token
     */
    void updateUserFCMToken(Long userId, String fcmToken);
    
    /**
     * Remove user's FCM token
     */
    void removeUserFCMToken(Long userId);
}
