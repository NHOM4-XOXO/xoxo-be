package com.nhom4.xoxo.kafka;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.nhom4.xoxo.chat.MongoChatMessage;
import com.nhom4.xoxo.dto.req.PushNotificationRequest;
import com.nhom4.xoxo.entity.ChatRoom;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MessageType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.ChatRoomRepository;
import com.nhom4.xoxo.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatConsumer {

    private final ChatRoomRepository chatRoomRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @KafkaListener(topics = "chat-messages", groupId = "chat-group")
    public void consumeChatMessage(MongoChatMessage message) {
        try {
            log.info("Processing chat message: room={} msgId={} sender={}", message.getChatRoomId(), message.getMessageId(), message.getSenderId());

            ChatRoom room = chatRoomRepository.findById(message.getChatRoomId())
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

            List<Long> recipients = room.getParticipants().stream()
                .map(User::getId)
                .filter(id -> !id.equals(message.getSenderId()))
                .collect(Collectors.toList());

            if (!recipients.isEmpty()) {
                PushNotificationRequest req = PushNotificationRequest.builder()
                    .title(message.getSenderName() != null ? message.getSenderName() : "New message")
                    .body(buildPreview(message))
                    .notificationData(PushNotificationRequest.NotificationData.builder()
                        .type("CHAT")
                        .chatRoomId(message.getChatRoomId())
                        .senderId(message.getSenderId())
                        .senderName(message.getSenderName())
                        .messagePreview(buildPreview(message))
                        .messageId(message.getMessageId())
                        .build())
                    .build();

                if (pushNotificationService != null) {
                    pushNotificationService.sendToUsers(recipients, req);
                } else {
                    log.debug("PushNotificationService not configured. Skipping push send.");
                }
            }

            log.info("Chat message processed successfully: {}", message.getMessageId());
        } catch (Exception e) {
            log.error("Error processing chat message: {}", message.getMessageId(), e);
        }
    }

    private String buildPreview(MongoChatMessage message) {
        if (message.getType() == MessageType.FILE) {
            return "[File] " + (message.getContent() != null ? message.getContent() : "");
        }
        return message.getContent() != null ? message.getContent() : "";
    }
}
