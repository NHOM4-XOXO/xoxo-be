package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.notification.MongoNotification;
import com.nhom4.xoxo.notification.MongoNotificationRepository;
import com.nhom4.xoxo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final MongoNotificationRepository mongoNotificationRepository;

    /**
     * Lấy notifications từ MySQL (persistent storage) - cho phân trang, thống kê
     */
    @GetMapping("/mysql")
    public ResponseEntity<WrapRes<Page<Notification>>> getNotificationsFromMySQL(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(WrapRes.success(notifications));
    }

    /**
     * Lấy notifications từ MongoDB (real-time storage) - cho hiển thị ngay lập tức
     */
    @GetMapping("/mongo")
    public ResponseEntity<WrapRes<List<MongoNotification>>> getNotificationsFromMongo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MongoNotification> notifications = mongoNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return ResponseEntity.ok(WrapRes.success(notifications.getContent()));
    }

    /**
     * Lấy số lượng notifications chưa đọc từ MySQL
     */
    @GetMapping("/mysql/unread/count")
    public ResponseEntity<WrapRes<Long>> getUnreadCountFromMySQL() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        Long count = notificationService.countUserUnreadNotifications(userId);
        return ResponseEntity.ok(WrapRes.success(count));
    }

    /**
     * Lấy số lượng notifications chưa đọc từ MongoDB
     */
    @GetMapping("/mongo/unread/count")
    public ResponseEntity<WrapRes<Long>> getUnreadCountFromMongo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        Long count = mongoNotificationRepository.countUnreadByUserId(userId);
        return ResponseEntity.ok(WrapRes.success(count));
    }

    /**
     * Lấy notifications chưa đọc từ MySQL
     */
    @GetMapping("/mysql/unread")
    public ResponseEntity<WrapRes<List<Notification>>> getUnreadNotificationsFromMySQL() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        List<Notification> notifications = notificationService.getUserUnreadNotifications(userId);
        return ResponseEntity.ok(WrapRes.success(notifications));
    }

    /**
     * Lấy notifications chưa đọc từ MongoDB
     */
    @GetMapping("/mongo/unread")
    public ResponseEntity<WrapRes<List<MongoNotification>>> getUnreadNotificationsFromMongo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        List<MongoNotification> notifications = mongoNotificationRepository.findUnreadByUserId(userId);
        return ResponseEntity.ok(WrapRes.success(notifications));
    }

    /**
     * Đánh dấu notification đã đọc trong MySQL
     */
    @PutMapping("/mysql/{id}/read")
    public ResponseEntity<WrapRes<String>> markNotificationAsReadInMySQL(@PathVariable Long id) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.ok(WrapRes.success("Notification marked as read"));
    }

    /**
     * Đánh dấu tất cả notifications đã đọc trong MySQL
     */
    @PutMapping("/mysql/read-all")
    public ResponseEntity<WrapRes<String>> markAllNotificationsAsReadInMySQL() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        notificationService.markAllUserNotificationsAsRead(userId);
        return ResponseEntity.ok(WrapRes.success("All notifications marked as read"));
    }

    /**
     * Đánh dấu notification đã đọc trong MongoDB
     */
    @PutMapping("/mongo/{id}/read")
    public ResponseEntity<WrapRes<String>> markNotificationAsReadInMongo(@PathVariable String id) {
        MongoNotification notification = mongoNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        notification.setReadAt(java.time.Instant.now());
        mongoNotificationRepository.save(notification);
        
        return ResponseEntity.ok(WrapRes.success("MongoDB notification marked as read"));
    }

    /**
     * Lấy notifications theo type từ MongoDB
     */
    @GetMapping("/mongo/type/{type}")
    public ResponseEntity<WrapRes<List<MongoNotification>>> getNotificationsByTypeFromMongo(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        List<MongoNotification> notifications = mongoNotificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        
        // Simple pagination for MongoDB
        int start = page * size;
        int end = Math.min(start + size, notifications.size());
        List<MongoNotification> paginatedNotifications = notifications.subList(start, end);
        
        return ResponseEntity.ok(WrapRes.success(paginatedNotifications));
    }

    /**
     * Xóa notification từ MySQL
     */
    @DeleteMapping("/mysql/{id}")
    public ResponseEntity<WrapRes<String>> deleteNotificationFromMySQL(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(WrapRes.success("Notification deleted from MySQL"));
    }

    /**
     * Xóa notification từ MongoDB
     */
    @DeleteMapping("/mongo/{id}")
    public ResponseEntity<WrapRes<String>> deleteNotificationFromMongo(@PathVariable String id) {
        mongoNotificationRepository.deleteById(id);
        return ResponseEntity.ok(WrapRes.success("Notification deleted from MongoDB"));
    }
}
