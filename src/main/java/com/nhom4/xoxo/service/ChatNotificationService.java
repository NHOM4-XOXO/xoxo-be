package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.res.ChatMessageResponse;

public interface ChatNotificationService {
    
    /**
     * Send notification to all participants in a chat room
     */
    void notifyChatRoomParticipants(Long chatRoomId, ChatMessageResponse message);
    
    /**
     * Send notification to specific user
     */
    void notifyUser(Long userId, ChatMessageResponse message);
    
    /**
     * Send typing indicator
     */
    void sendTypingIndicator(Long chatRoomId, Long userId, boolean isTyping);
    
    /**
     * Send online/offline status
     */
    void sendUserStatus(Long userId, boolean isOnline);
}
