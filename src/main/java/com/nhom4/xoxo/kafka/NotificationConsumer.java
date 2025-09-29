package com.nhom4.xoxo.kafka;

import com.nhom4.xoxo.dto.req.NotificationMessage;
import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.notification.MongoNotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.nhom4.xoxo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {
    
    private final MongoNotificationRepository mongoNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    
    @KafkaListener(topics = "${notification.topic:notifications}", groupId = "notification-group")
    public void listen(NotificationMessage message) {
        try {
            MongoNotification notification = MongoNotification.builder()
                .userId(message.getUserId())
                .message(message.getMessage())
                .type(message.getType())
                .targetId(message.getTargetId())
                .targetType(message.getTargetType())
                .senderId(message.getSenderId())
                .actionType(message.getActionType())
                .payload(message.getPayload())
                .read(false)
                .createdAt(Instant.now())
                .build();
            
            MongoNotification saved = mongoNotificationRepository.save(notification);
            log.info("[NotificationConsumer] Saved notification for user {}: {}", 
                message.getUserId(), message.getMessage());
            
            // Broadcast to the specific user over STOMP for real-time updates
            try {
                // convertAndSendToUser resolves by Principal.getName() (username/email)
                // We must use the same identifier as JWT subject / UserDetails#getUsername
                String usernameOrEmail = null;
                try {
                    var user = userService.findById(saved.getUserId());
                    usernameOrEmail = user != null ? user.getEmail() : null;
                } catch (Exception ignored) {}

                messagingTemplate.convertAndSendToUser(
                    usernameOrEmail != null ? usernameOrEmail : String.valueOf(saved.getUserId()),
                    "/queue/notifications",
                    saved
                );
            } catch (Exception e) {
                log.error("[NotificationConsumer] Failed to broadcast notification via WS: {}", e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("[NotificationConsumer] Failed to save notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save notification", e);
        }
    }
}