package com.nhom4.xoxo.service;

import java.util.List;

import com.nhom4.xoxo.entity.Notification;

public interface NotificationService {
    Notification creatNotification(Notification notification);

    List<Notification> getAllNotifications();

    Notification getNotificationById(Long id);

    Notification updateNotification(Long id, Notification notification);

    void deleteNotification(Long id);
    
    List<Notification> list(Long userId);
}
