package com.nhom4.xoxo.kafka;

import com.nhom4.xoxo.dto.req.NotificationMessage;
import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.notification.MongoNotificationRepository;
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
            
            mongoNotificationRepository.save(notification);
            log.info("[NotificationConsumer] Saved notification for user {}: {}", 
                message.getUserId(), message.getMessage());
            
        } catch (Exception e) {
            log.error("[NotificationConsumer] Failed to save notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save notification", e);
        }
    }
}