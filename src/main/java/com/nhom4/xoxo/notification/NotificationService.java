package com.nhom4.xoxo.notification;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public Notification send(Long recipientUserId, String type, String payloadJson) {
        Notification n = new Notification();
        n.setUserId(recipientUserId);
        n.setType(type);
        n.setPayload(payloadJson);
        n.setRead(false);
        return notificationRepository.save(n);
    }

    public List<Notification> list(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}



