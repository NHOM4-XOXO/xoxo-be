package com.nhom4.xoxo.chat;

import com.nhom4.xoxo.service.MessengerChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessengerWebSocketHandler {

    private final MessengerChatService messengerChatService;
    
    // Track user sessions and subscriptions
    private final ConcurrentMap<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> userOnlineStatus = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String userEmail = user.getName();
            Long userId = extractUserIdFromEmail(userEmail);
            
            sessionUserMap.put(sessionId, userEmail);
            userOnlineStatus.put(userEmail, userId);
            
            // Update user online status
            messengerChatService.updateUserOnlineStatus(userId, true);
            
            log.info("User connected: {} with session: {}", userEmail, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        String userEmail = sessionUserMap.remove(sessionId);
        if (userEmail != null) {
            Long userId = userOnlineStatus.remove(userEmail);
            
            if (userId != null) {
                // Update user offline status and last seen
                messengerChatService.updateUserOnlineStatus(userId, false);
                messengerChatService.updateLastSeen(userId);
                
                // Stop all typing indicators for this user
                stopAllTypingForUser(userId);
            }
            
            log.info("User disconnected: {} with session: {}", userEmail, sessionId);
        }
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("User subscribed to: {} with session: {}", destination, sessionId);
        
        // Handle specific subscription types
        if (destination != null) {
            if (destination.startsWith("/topic/chat/")) {
                // User subscribed to a chat room
                String chatRoomId = extractChatRoomIdFromDestination(destination);
                handleChatRoomSubscription(sessionId, chatRoomId);
            } else if (destination.startsWith("/user/queue/")) {
                // User subscribed to personal notifications
                handlePersonalSubscription(sessionId);
            }
        }
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("User unsubscribed from: {} with session: {}", destination, sessionId);
        
        if (destination != null && destination.startsWith("/topic/chat/")) {
            String chatRoomId = extractChatRoomIdFromDestination(destination);
            handleChatRoomUnsubscription(sessionId, chatRoomId);
        }
    }

    private void handleChatRoomSubscription(String sessionId, String chatRoomId) {
        String userEmail = sessionUserMap.get(sessionId);
        if (userEmail != null) {
            Long userId = userOnlineStatus.get(userEmail);
            if (userId != null) {
                try {
                    Long roomId = Long.parseLong(chatRoomId);
                    // Mark user as active in this chat room
                    log.info("User {} joined chat room: {}", userId, roomId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid chat room ID: {}", chatRoomId);
                }
            }
        }
    }

    private void handleChatRoomUnsubscription(String sessionId, String chatRoomId) {
        String userEmail = sessionUserMap.get(sessionId);
        if (userEmail != null) {
            Long userId = userOnlineStatus.get(userEmail);
            if (userId != null) {
                try {
                    Long roomId = Long.parseLong(chatRoomId);
                    // Stop typing indicator when leaving room
                    messengerChatService.stopTyping(roomId, userId);
                    log.info("User {} left chat room: {}", userId, roomId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid chat room ID: {}", chatRoomId);
                }
            }
        }
    }

    private void handlePersonalSubscription(String sessionId) {
        String userEmail = sessionUserMap.get(sessionId);
        if (userEmail != null) {
            log.debug("User {} subscribed to personal notifications", userEmail);
        }
    }

    private void stopAllTypingForUser(Long userId) {
        // Implementation would stop typing indicators for all rooms where user was typing
        // This would require tracking which rooms user is typing in
        log.debug("Stopping all typing indicators for user: {}", userId);
    }

    private String extractChatRoomIdFromDestination(String destination) {
        // Extract room ID from "/topic/chat/{roomId}" or "/topic/chat/{roomId}/typing"
        String[] parts = destination.split("/");
        if (parts.length >= 4) {
            return parts[3]; // chat room ID
        }
        return null;
    }

    private Long extractUserIdFromEmail(String email) {
        // This is a simplified extraction - in reality, you'd look up the user
        // For now, assume email format includes user ID or look up from database
        try {
            // If email format is "userId@domain.com"
            if (email.contains("@")) {
                String[] parts = email.split("@");
                return Long.parseLong(parts[0]);
            }
        } catch (NumberFormatException e) {
            log.warn("Could not extract user ID from email: {}", email);
        }
        return null;
    }

    // Public methods for external access
    public boolean isUserOnline(String userEmail) {
        return userOnlineStatus.containsKey(userEmail);
    }

    public int getOnlineUserCount() {
        return userOnlineStatus.size();
    }
}







