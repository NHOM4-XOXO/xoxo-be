package com.nhom4.xoxo.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void sendUnreadCount(Long userId, long unreadCount) {
        try {
            String principal = resolvePrincipalName(userId);
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                    principal,
                    "/queue/notifications/unread-count",
                    unreadCount
                );
            }
        } catch (Exception ex) {
            log.error("Failed to send unread count to user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    public void sendNewNotification(Long userId, Notification notification) {
        try {
            String principal = resolvePrincipalName(userId);
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                    principal,
                    "/queue/notifications",
                    notification
                );
            }
        } catch (Exception ex) {
            log.error("Failed to send notification to user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    public void sendNewNotification(Long userId, MongoNotification notification) {
        try {
            String principal = resolvePrincipalName(userId);
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                    principal,
                    "/queue/notifications",
                    notification
                );
            }
        } catch (Exception ex) {
            log.error("Failed to send mongo notification to user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    private String resolvePrincipalName(Long userId) {
        return userRepository.findById(userId).map(u -> u.getEmail()).orElse(null);
    }
}


