package com.nhom4.xoxo.service.impl;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.service.ChatNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatNotificationServiceImpl implements ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyChatRoomParticipants(Long chatRoomId, ChatMessageResponse message) {
        try {
            // Send to all participants in the chat room
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, message);
            log.info("Notification sent to chat room: {}", chatRoomId);
        } catch (Exception e) {
            log.error("Error sending notification to chat room: {}", chatRoomId, e);
        }
    }

    @Override
    public void notifyUser(Long userId, ChatMessageResponse message) {
        try {
            // Send to specific user's queue
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/chat/" + message.getChatRoomId(),
                message
            );
            log.info("Notification sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending notification to user: {}", userId, e);
        }
    }

    @Override
    public void sendTypingIndicator(Long chatRoomId, Long userId, boolean isTyping) {
        try {
            // Send typing indicator to all participants
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/typing", 
                new TypingIndicator(userId, isTyping));
            log.info("Typing indicator sent for user: {} in room: {}", userId, chatRoomId);
        } catch (Exception e) {
            log.error("Error sending typing indicator for user: {} in room: {}", userId, chatRoomId, e);
        }
    }

    @Override
    public void sendUserStatus(Long userId, boolean isOnline) {
        try {
            // Send user status to all users (or specific users who are friends)
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", 
                new UserStatus(userId, isOnline));
            log.info("User status sent for user: {} - online: {}", userId, isOnline);
        } catch (Exception e) {
            log.error("Error sending user status for user: {}", userId, e);
        }
    }

    // Inner classes for structured messages
    public static class TypingIndicator {
        private Long userId;
        private boolean isTyping;

        public TypingIndicator(Long userId, boolean isTyping) {
            this.userId = userId;
            this.isTyping = isTyping;
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public boolean isTyping() { return isTyping; }
        public void setTyping(boolean isTyping) { this.isTyping = isTyping; }
    }

    public static class UserStatus {
        private Long userId;
        private boolean isOnline;

        public UserStatus(Long userId, boolean isOnline) {
            this.userId = userId;
            this.isOnline = isOnline;
        }

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public boolean isOnline() { return isOnline; }
        public void setOnline(boolean isOnline) { this.isOnline = isOnline; }
    }
}
