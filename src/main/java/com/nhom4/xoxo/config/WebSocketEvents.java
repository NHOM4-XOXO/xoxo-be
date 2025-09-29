package com.nhom4.xoxo.config;

import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import com.nhom4.xoxo.service.NotificationService;
import com.nhom4.xoxo.service.NotificationWebSocketService;
import com.nhom4.xoxo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEvents {

    private final NotificationService notificationService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final UserRepository userRepository;

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        try {
            Object principalObj = event.getUser();
            if (principalObj instanceof Authentication auth) {
                String email = auth.getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    Long unread = notificationService.countUserUnreadNotifications(user.getId());
                    notificationWebSocketService.sendUnreadCount(user.getId(), unread);
                });
            }
        } catch (Exception ex) {
            log.warn("onConnect failed: {}", ex.getMessage());
        }
    }
}


