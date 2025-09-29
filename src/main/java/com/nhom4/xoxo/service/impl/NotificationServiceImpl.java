package com.nhom4.xoxo.service.impl;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.entity.NotificationType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.kafka.NotificationProducer;
import com.nhom4.xoxo.repository.NotificationRepository;
import com.nhom4.xoxo.service.NotificationWebSocketService;
import com.nhom4.xoxo.service.NotificationService;
import com.nhom4.xoxo.dto.req.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProducer notificationProducer;
    private final NotificationWebSocketService notificationWebSocketService;

    @Override
    public Notification createNotification(Notification notification) {
        // Lưu vào MySQL
        Notification savedNotification = notificationRepository.save(notification);
        
        // Gửi qua Kafka để lưu vào MongoDB (real-time)
        try {
            NotificationMessage message = NotificationMessage.builder()
                .userId(notification.getUserId())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .targetId(notification.getTargetId())
                .targetType(notification.getTargetType())
                .senderId(notification.getSenderId())
                .actionType(notification.getActionType())
                .payload(notification.getPayload())
                .build();
            
            notificationProducer.sendNotification(message);
        } catch (Exception e) {
            log.error("Failed to send notification via Kafka: {}", e.getMessage());
            // Không throw exception để không ảnh hưởng đến MySQL operation
        }
        
        // Push realtime to the user over WebSocket
        try {
            notificationWebSocketService.sendNewNotification(notification.getUserId(), savedNotification);
            Long unread = notificationRepository.countUnreadByUserId(notification.getUserId());
            notificationWebSocketService.sendUnreadCount(notification.getUserId(), unread);
        } catch (Exception ex) {
            log.warn("Failed to push websocket notification: {}", ex.getMessage());
        }
        
        return savedNotification;
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
    }

    @Override
    public Notification updateNotification(Long id, Notification notification) {
        Notification existing = getNotificationById(id);
        existing.setMessage(notification.getMessage());
        existing.setIsRead(notification.getIsRead());
        existing.setType(notification.getType());
        existing.setTargetId(notification.getTargetId());
        existing.setTargetType(notification.getTargetType());
        existing.setSenderId(notification.getSenderId());
        existing.setActionType(notification.getActionType());
        existing.setPayload(notification.getPayload());
        existing.setUpdatedAt(LocalDateTime.now());
        return notificationRepository.save(existing);
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
    
    @Override
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    @Override
    public List<Notification> getUserUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }
    
    @Override
    public Long countUserUnreadNotifications(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Override
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = getNotificationById(notificationId);
        if (notification == null) {
            throw new NotFoundException("Notification not found");
        }
        notification.setIsRead(true);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        try {
            Long unread = notificationRepository.countUnreadByUserId(notification.getUserId());
            notificationWebSocketService.sendUnreadCount(notification.getUserId(), unread);
        } catch (Exception ex) {
            log.warn("Failed to push unread count after read: {}", ex.getMessage());
        }
    }
    
    @Override
    public void markAllUserNotificationsAsRead(Long userId) {
        List<Notification> unreadNotifications = getUserUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setUpdatedAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(unreadNotifications);
        try {
            notificationWebSocketService.sendUnreadCount(userId, 0L);
        } catch (Exception ex) {
            log.warn("Failed to push unread count after read-all: {}", ex.getMessage());
        }
    }
    
    @Override
    public void sendFriendRequestNotification(Long senderId, Long recipientId) {
        Notification notification = Notification.builder()
            .userId(recipientId)
            .senderId(senderId)
            .type(NotificationType.FRIEND_REQUEST)
            .targetType("USER")
            .targetId(senderId)
            .actionType("FRIEND_REQUEST")
            .message("Bạn có lời mời kết bạn mới")
            .isRead(false)
            .build();
        
        createNotification(notification);
    }
    
    @Override
    public void sendPostLikeNotification(Long postId, Long postOwnerId, Long likerId) {
        Notification notification = Notification.builder()
            .userId(postOwnerId)
            .senderId(likerId)
            .type(NotificationType.POST_LIKE)
            .targetType("POST")
            .targetId(postId)
            .actionType("LIKE")
            .message("Bài viết của bạn được thích")
            .isRead(false)
            .build();
        
        createNotification(notification);
    }
    
    @Override
    public void sendPostCommentNotification(Long postId, Long postOwnerId, Long commenterId) {
        Notification notification = Notification.builder()
            .userId(postOwnerId)
            .senderId(commenterId)
            .type(NotificationType.POST_COMMENT)
            .targetType("POST")
            .targetId(postId)
            .actionType("COMMENT")
            .message("Bài viết của bạn có comment mới")
            .isRead(false)
            .build();
        
        createNotification(notification);
    }
    
    @Override
    public void sendGroupInviteNotification(Long groupId, Long groupOwnerId, Long inviteeId) {
        Notification notification = Notification.builder()
            .userId(inviteeId)
            .senderId(groupOwnerId)
            .type(NotificationType.GROUP_INVITE)
            .targetType("GROUP")
            .targetId(groupId)
            .actionType("INVITE")
            .message("Bạn được mời tham gia nhóm")
            .isRead(false)
            .build();
        
        createNotification(notification);
    }
    
    @Override
    public void sendPostShareNotification(Long postId, Long postOwnerId, Long sharerId) {
        Notification notification = Notification.builder()
            .userId(postOwnerId)
            .senderId(sharerId)
            .type(NotificationType.POST_SHARE)
            .targetType("POST")
            .targetId(postId)
            .actionType("SHARE")
            .message("Bài viết của bạn được chia sẻ")
            .isRead(false)
            .build();
        
        createNotification(notification);
    }
}