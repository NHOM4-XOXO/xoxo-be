package com.nhom4.xoxo.service;

import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.notification.MongoNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service để xử lý real-time notifications từ MongoDB
 * Cung cấp các phương thức truy vấn nhanh cho real-time features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeNotificationService {

    private final MongoNotificationRepository mongoNotificationRepository;

    /**
     * Lấy notifications real-time cho user (từ MongoDB)
     * Sử dụng cho hiển thị ngay lập tức
     */
    public List<MongoNotification> getRealTimeNotifications(Long userId, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<MongoNotification> notifications = 
                mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            return notifications.getContent();
        } catch (Exception e) {
            log.error("Failed to get real-time notifications for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Lấy notifications chưa đọc real-time
     */
    public List<MongoNotification> getUnreadRealTimeNotifications(Long userId) {
        try {
            return mongoNotificationRepository.findUnreadByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to get unread real-time notifications for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Đếm số notifications chưa đọc real-time
     */
    public long countUnreadRealTimeNotifications(Long userId) {
        try {
            return mongoNotificationRepository.countUnreadByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to count unread real-time notifications for user {}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Lấy notifications theo type real-time
     */
    public List<MongoNotification> getRealTimeNotificationsByType(Long userId, String type, int limit) {
        try {
            List<MongoNotification> notifications = 
                mongoNotificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
            
            // Simple pagination
            return notifications.stream()
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get real-time notifications by type for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Lấy notifications mới nhất (trong vòng X phút)
     */
    public List<MongoNotification> getRecentNotifications(Long userId, int minutesAgo, int limit) {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(minutesAgo * 60L);
            
            // Lấy tất cả notifications và filter theo thời gian
            Page<MongoNotification> allNotificationsPage = 
                mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000));
            
            return allNotificationsPage.getContent().stream()
                .filter(notification -> notification.getCreatedAt().isAfter(cutoffTime))
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get recent notifications for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Lấy notifications theo target (ví dụ: notifications liên quan đến một post cụ thể)
     */
    public List<MongoNotification> getNotificationsByTarget(Long userId, String targetType, Long targetId, int limit) {
        try {
            // Lấy tất cả notifications và filter theo target
            Page<MongoNotification> allNotificationsPage = 
                mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000));
            
            return allNotificationsPage.getContent().stream()
                .filter(notification -> 
                    targetType.equals(notification.getTargetType()) && 
                    targetId.equals(notification.getTargetId()))
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get notifications by target for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Lấy notifications từ một user cụ thể
     */
    public List<MongoNotification> getNotificationsFromSender(Long userId, Long senderId, int limit) {
        try {
            // Lấy tất cả notifications và filter theo sender
            Page<MongoNotification> allNotificationsPage = 
                mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000));
            
            return allNotificationsPage.getContent().stream()
                .filter(notification -> senderId.equals(notification.getSenderId()))
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get notifications from sender for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Đánh dấu notification đã đọc real-time
     */
    public boolean markNotificationAsReadRealTime(String notificationId) {
        try {
            Optional<MongoNotification> notification = mongoNotificationRepository.findById(notificationId);
            if (notification.isPresent()) {
                MongoNotification mongoNotification = notification.get();
                mongoNotification.setRead(true);
                mongoNotification.setReadAt(Instant.now());
                mongoNotificationRepository.save(mongoNotification);
                log.debug("Marked notification as read: {}", notificationId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Đánh dấu tất cả notifications của user đã đọc real-time
     */
    public boolean markAllNotificationsAsReadRealTime(Long userId) {
        try {
            List<MongoNotification> unreadNotifications = 
                mongoNotificationRepository.findUnreadByUserId(userId);
            
            unreadNotifications.forEach(notification -> {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
            });
            
            mongoNotificationRepository.saveAll(unreadNotifications);
            log.debug("Marked all notifications as read for user: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to mark all notifications as read for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Xóa notification real-time
     */
    public boolean deleteRealTimeNotification(String notificationId) {
        try {
            mongoNotificationRepository.deleteById(notificationId);
            log.debug("Deleted real-time notification: {}", notificationId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete real-time notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Xóa tất cả notifications của user real-time
     */
    public boolean deleteAllUserNotificationsRealTime(Long userId) {
        try {
            Page<MongoNotification> allNotificationsPage = 
                mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10000));
            
            mongoNotificationRepository.deleteAll(allNotificationsPage.getContent());
            log.debug("Deleted all real-time notifications for user: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete all real-time notifications for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy thống kê notifications real-time
     */
    public NotificationStats getNotificationStats(Long userId) {
        try {
            // Sử dụng count() với filter thay vì countByUserId
            long totalCount = mongoNotificationRepository.count();
            long unreadCount = mongoNotificationRepository.countUnreadByUserId(userId);
            long readCount = totalCount - unreadCount;
            
            return new NotificationStats(totalCount, unreadCount, readCount);
        } catch (Exception e) {
            log.error("Failed to get notification stats for user {}: {}", userId, e.getMessage(), e);
            return new NotificationStats(0, 0, 0);
        }
    }

    /**
     * Inner class để chứa thống kê notifications
     */
    public static class NotificationStats {
        private final long totalCount;
        private final long unreadCount;
        private final long readCount;

        public NotificationStats(long totalCount, long unreadCount, long readCount) {
            this.totalCount = totalCount;
            this.unreadCount = unreadCount;
            this.readCount = readCount;
        }

        public long getTotalCount() { return totalCount; }
        public long getUnreadCount() { return unreadCount; }
        public long getReadCount() { return readCount; }
    }
}
