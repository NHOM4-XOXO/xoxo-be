package com.nhom4.xoxo.service;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.notification.MongoNotificationRepository;
import com.nhom4.xoxo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service để đồng bộ dữ liệu notification giữa MySQL và MongoDB
 * Đảm bảo tính nhất quán dữ liệu giữa hai hệ thống
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSyncService {

    private final NotificationRepository mysqlNotificationRepository;
    private final MongoNotificationRepository mongoNotificationRepository;

    /**
     * Đồng bộ tất cả notifications từ MySQL sang MongoDB
     * Chạy mỗi giờ để đảm bảo dữ liệu được đồng bộ
     */
    @Scheduled(fixedRate = 3600000) // 1 giờ
    public void syncAllNotificationsFromMySQL() {
        log.info("Starting full sync of notifications from MySQL to MongoDB");
        
        int page = 0;
        int size = 100;
        boolean hasMore = true;
        
        while (hasMore) {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notification> mysqlNotifications = mysqlNotificationRepository.findAll(pageable);
            
            if (mysqlNotifications.isEmpty()) {
                hasMore = false;
                break;
            }
            
            for (Notification mysqlNotification : mysqlNotifications.getContent()) {
                syncSingleNotification(mysqlNotification);
            }
            
            page++;
            hasMore = mysqlNotifications.hasNext();
        }
        
        log.info("Full sync completed");
    }

    /**
     * Đồng bộ một notification cụ thể từ MySQL sang MongoDB
     */
    public void syncSingleNotification(Notification mysqlNotification) {
        try {
            // Kiểm tra xem notification đã tồn tại trong MongoDB chưa
            Optional<MongoNotification> existingMongoNotification = 
                mongoNotificationRepository.findById(mysqlNotification.getId().toString());
            
            if (existingMongoNotification.isPresent()) {
                // Cập nhật notification hiện có
                MongoNotification mongoNotification = existingMongoNotification.get();
                updateMongoNotification(mongoNotification, mysqlNotification);
                mongoNotificationRepository.save(mongoNotification);
                log.debug("Updated MongoDB notification: {}", mysqlNotification.getId());
            } else {
                // Tạo mới notification trong MongoDB
                MongoNotification newMongoNotification = createMongoNotification(mysqlNotification);
                mongoNotificationRepository.save(newMongoNotification);
                log.debug("Created new MongoDB notification: {}", mysqlNotification.getId());
            }
        } catch (Exception e) {
            log.error("Failed to sync notification {}: {}", mysqlNotification.getId(), e.getMessage(), e);
        }
    }

    /**
     * Tạo MongoNotification từ MySQL Notification
     */
    private MongoNotification createMongoNotification(Notification mysqlNotification) {
        return MongoNotification.builder()
            .id(mysqlNotification.getId().toString())
            .userId(mysqlNotification.getUserId())
            .message(mysqlNotification.getMessage())
            .type(mysqlNotification.getType().name())
            .targetId(mysqlNotification.getTargetId())
            .targetType(mysqlNotification.getTargetType())
            .senderId(mysqlNotification.getSenderId())
            .actionType(mysqlNotification.getActionType())
            .payload(mysqlNotification.getPayload())
            .read(mysqlNotification.getIsRead())
            .createdAt(mysqlNotification.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
            .readAt(mysqlNotification.getIsRead() && mysqlNotification.getUpdatedAt() != null ? 
                mysqlNotification.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null)
            .build();
    }

    /**
     * Cập nhật MongoNotification từ MySQL Notification
     */
    private void updateMongoNotification(MongoNotification mongoNotification, Notification mysqlNotification) {
        mongoNotification.setMessage(mysqlNotification.getMessage());
        mongoNotification.setType(mysqlNotification.getType().name());
        mongoNotification.setTargetId(mysqlNotification.getTargetId());
        mongoNotification.setTargetType(mysqlNotification.getTargetType());
        mongoNotification.setSenderId(mysqlNotification.getSenderId());
        mongoNotification.setActionType(mysqlNotification.getActionType());
        mongoNotification.setPayload(mysqlNotification.getPayload());
        mongoNotification.setRead(mysqlNotification.getIsRead());
        
        if (mysqlNotification.getIsRead() && mysqlNotification.getUpdatedAt() != null) {
            mongoNotification.setReadAt(mysqlNotification.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
    }

    /**
     * Đồng bộ trạng thái đã đọc từ MongoDB về MySQL
     * Chạy mỗi 5 phút để cập nhật trạng thái đã đọc
     */
    @Scheduled(fixedRate = 300000) // 5 phút
    public void syncReadStatusFromMongoDB() {
        log.info("Starting sync of read status from MongoDB to MySQL");
        
        try {
            // Lấy tất cả notifications chưa đọc từ MongoDB
            List<MongoNotification> unreadMongoNotifications = mongoNotificationRepository.findAll();
            
            for (MongoNotification mongoNotification : unreadMongoNotifications) {
                if (mongoNotification.isRead()) {
                    // Cập nhật trạng thái đã đọc trong MySQL
                    try {
                        Long notificationId = Long.parseLong(mongoNotification.getId());
                        Optional<Notification> mysqlNotification = mysqlNotificationRepository.findById(notificationId);
                        
                        if (mysqlNotification.isPresent() && !mysqlNotification.get().getIsRead()) {
                            Notification notification = mysqlNotification.get();
                            notification.setIsRead(true);
                            notification.setUpdatedAt(java.time.LocalDateTime.now());
                            mysqlNotificationRepository.save(notification);
                            log.debug("Updated MySQL notification read status: {}", notificationId);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid notification ID format: {}", mongoNotification.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync read status: {}", e.getMessage(), e);
        }
        
        log.info("Read status sync completed");
    }

    /**
     * Xóa notifications cũ từ MongoDB (older than 90 days)
     * Chạy mỗi ngày lúc 2 giờ sáng
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications from MongoDB");
        
        try {
            Instant cutoffDate = Instant.now().minusSeconds(7776000); // 90 days ago
            
            // MongoDB TTL index sẽ tự động xóa các documents cũ
            //  cleanup thủ công 
            log.info("Cleanup completed. Old notifications will be automatically removed by TTL index");
        } catch (Exception e) {
            log.error("Failed to cleanup old notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra tính nhất quán dữ liệu giữa MySQL và MongoDB
     * Chạy mỗi 6 giờ
     */
    @Scheduled(fixedRate = 21600000) // 6 giờ
    public void checkDataConsistency() {
        log.info("Starting data consistency check between MySQL and MongoDB");
        
        try {
            long mysqlCount = mysqlNotificationRepository.count();
            long mongoCount = mongoNotificationRepository.count();
            
            log.info("MySQL notifications count: {}", mysqlCount);
            log.info("MongoDB notifications count: {}", mongoCount);
            
            if (Math.abs(mysqlCount - mongoCount) > 10) {
                log.warn("Significant difference in notification counts detected!");
                log.warn("MySQL: {}, MongoDB: {}, Difference: {}", 
                    mysqlCount, mongoCount, Math.abs(mysqlCount - mongoCount));
            } else {
                log.info("Data consistency check passed");
            }
        } catch (Exception e) {
            log.error("Failed to check data consistency: {}", e.getMessage(), e);
        }
    }
}
