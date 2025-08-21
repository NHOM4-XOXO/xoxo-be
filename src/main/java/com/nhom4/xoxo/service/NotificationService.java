package com.nhom4.xoxo.service;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    
    // MySQL operations
    Notification createNotification(Notification notification);
    List<Notification> getAllNotifications();
    Notification getNotificationById(Long id);
    Notification updateNotification(Long id, Notification notification);
    void deleteNotification(Long id);
    
    // User-specific operations
    Page<Notification> getUserNotifications(Long userId, Pageable pageable);
    List<Notification> getUserUnreadNotifications(Long userId);
    Long countUserUnreadNotifications(Long userId);
    void markNotificationAsRead(Long notificationId);
    void markAllUserNotificationsAsRead(Long userId);
    
    // Business logic methods
    void sendFriendRequestNotification(Long senderId, Long recipientId);
    void sendPostLikeNotification(Long postId, Long postOwnerId, Long likerId);
    void sendPostCommentNotification(Long postId, Long postOwnerId, Long commenterId);
    void sendPostShareNotification(Long postId, Long postOwnerId, Long sharerId);
    void sendGroupInviteNotification(Long groupId, Long groupOwnerId, Long inviteeId);
}