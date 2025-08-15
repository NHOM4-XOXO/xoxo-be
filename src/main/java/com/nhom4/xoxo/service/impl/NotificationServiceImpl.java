package com.nhom4.xoxo.service.impl;

import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.repository.NotificationRepository;
import com.nhom4.xoxo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification creatNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    @Override
    public Notification updateNotification(Long id, Notification notification) {
        Notification existing = getNotificationById(id);
        existing.setMessage(notification.getMessage());
        existing.setIsRead(notification.getIsRead());
        existing.setType(notification.getType());
        existing.setTargetId(notification.getTargetId());
        return notificationRepository.save(existing);
    }

    @Override
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
