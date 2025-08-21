package com.nhom4.xoxo.service.impl;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom4.xoxo.chat.MongoChatMessage;
import com.nhom4.xoxo.chat.MongoChatMessageRepository;
import com.nhom4.xoxo.dto.req.ChatMessageRequest;
import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
import com.nhom4.xoxo.dto.req.PushNotificationRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.ChatRoomResponse;
import com.nhom4.xoxo.dto.res.FileUploadResponse;
import com.nhom4.xoxo.entity.ChatMessage;
import com.nhom4.xoxo.entity.ChatRoom;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.UserDevice;
import com.nhom4.xoxo.entity.UserEncryptionKey;
import com.nhom4.xoxo.enums.MessageType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.ChatMessageRepository;
import com.nhom4.xoxo.repository.ChatRoomRepository;
import com.nhom4.xoxo.repository.UserDeviceRepository;
import com.nhom4.xoxo.repository.UserEncryptionKeyRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.security.EncryptionService;
import com.nhom4.xoxo.service.ChatService;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.EnhancedChatService;
import com.nhom4.xoxo.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional("transactionManager")
public class EnhancedChatServiceImpl implements EnhancedChatService {

    private static final String CHAT_TOPIC = "chat-messages";

    private final ChatService baseChatService;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final MongoChatMessageRepository mongoChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EncryptionService encryptionService;
    private final UserEncryptionKeyRepository userEncryptionKeyRepository;
    private final CloudinaryService cloudinaryService;
    private final UserDeviceRepository userDeviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    // ===== Delegate to base chat service =====
    @Override
    public ChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId) {
        return baseChatService.createChatRoom(request, currentUserId);
    }

    @Override
    public ChatRoomResponse getChatRoomById(Long chatRoomId, Long currentUserId) {
        return baseChatService.getChatRoomById(chatRoomId, currentUserId);
    }

    @Override
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        return baseChatService.getUserChatRooms(userId);
    }

    @Override
    public ChatRoomResponse updateChatRoom(Long chatRoomId, CreateChatRoomRequest request, Long currentUserId) {
        return baseChatService.updateChatRoom(chatRoomId, request, currentUserId);
    }

    @Override
    public void deleteChatRoom(Long chatRoomId, Long currentUserId) {
        baseChatService.deleteChatRoom(chatRoomId, currentUserId);
    }

    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Long currentUserId) {
        return baseChatService.sendMessage(request, currentUserId);
    }

    @Override
    public Page<ChatMessageResponse> getChatMessages(Long chatRoomId, Long currentUserId, Pageable pageable) {
        return baseChatService.getChatMessages(chatRoomId, currentUserId, pageable);
    }

    @Override
    public ChatMessageResponse getMessageById(Long messageId, Long currentUserId) {
        return baseChatService.getMessageById(messageId, currentUserId);
    }

    @Override
    public void deleteMessage(Long messageId, Long currentUserId) {
        baseChatService.deleteMessage(messageId, currentUserId);
    }

    @Override
    public void addParticipant(Long chatRoomId, Long userId, Long currentUserId) {
        baseChatService.addParticipant(chatRoomId, userId, currentUserId);
    }

    @Override
    public void removeParticipant(Long chatRoomId, Long userId, Long currentUserId) {
        baseChatService.removeParticipant(chatRoomId, userId, currentUserId);
    }

    @Override
    public void leaveChatRoom(Long chatRoomId, Long currentUserId) {
        baseChatService.leaveChatRoom(chatRoomId, currentUserId);
    }

    @Override
    public void markMessageAsRead(Long messageId, Long currentUserId) {
        baseChatService.markMessageAsRead(messageId, currentUserId);
    }

    @Override
    public void markMessageAsDelivered(Long messageId, Long currentUserId) {
        baseChatService.markMessageAsDelivered(messageId, currentUserId);
    }

    @Override
    public Long getUnreadMessageCount(Long chatRoomId, Long currentUserId) {
        return baseChatService.getUnreadMessageCount(chatRoomId, currentUserId);
    }

    @Override
    public ChatRoomResponse getOrCreateDirectChat(Long otherUserId, Long currentUserId) {
        return baseChatService.getOrCreateDirectChat(otherUserId, currentUserId);
    }

    // ===== E2EE =====
    @Override
    public void generateUserEncryptionKeys(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        try {
            var pair = encryptionService.generateRSAKeyPair();
            String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            String pri = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
            String fp = Integer.toHexString(pub.hashCode());

            UserEncryptionKey k = userEncryptionKeyRepository.findByUserId(userId)
                .orElse(UserEncryptionKey.builder().user(user).build());
            k.setPublicKey(pub);
            k.setPrivateKey(pri);
            k.setKeyFingerprint(fp);
            k.setActive(true);
            userEncryptionKeyRepository.save(k);
        } catch (Exception e) {
            throw new RuntimeException("Generate keys failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getUserPublicKey(Long userId) {
        return userEncryptionKeyRepository.findByUserId(userId)
            .map(UserEncryptionKey::getPublicKey)
            .orElseThrow(() -> new NotFoundException("User does not have encryption key"));
    }

    @Override
    public ChatMessageResponse sendEncryptedMessage(ChatMessageRequest request, Long currentUserId) {
        Long chatRoomId = request.getChatRoomId();
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new NotFoundException("Chat room not found"));
        User sender = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Sender not found"));
        try {
            SecretKey aesKey = encryptionService.generateAESKey();
            var enc = encryptionService.encryptMessage(request.getContent(), aesKey);

            // Demo payload: {iv, data, k}
            Map<String, String> payload = Map.of(
                "iv", enc.getIv(),
                "data", enc.getEncryptedData(),
                "k", Base64.getEncoder().encodeToString(aesKey.getEncoded())
            );
            String encryptedJson = objectMapper.writeValueAsString(payload);

            ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .type(MessageType.TEXT)
                .content(encryptedJson)
                .sentAt(LocalDateTime.now())
                .delivered(false)
                .read(false)
                .deleted(false)
                .build();
            msg = chatMessageRepository.save(msg);

            MongoChatMessage mm = MongoChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .content(encryptedJson)
                .type(MessageType.TEXT)
                .sentAt(java.time.Instant.now())
                .senderName(getDisplayName(sender))
                .senderAvatar(sender.getAvatarUrl())
                .messageId(msg.getId())
                .build();
            mongoChatMessageRepository.save(mm);

            ChatMessageResponse res = ChatMessageResponse.builder()
                .id(msg.getId())
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .senderName(getDisplayName(sender))
                .senderAvatar(sender.getAvatarUrl())
                .content(encryptedJson)
                .type(MessageType.TEXT)
                .sentAt(msg.getSentAt())
                .delivered(false)
                .read(false)
                .build();

            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, res);
            kafkaTemplate.send(CHAT_TOPIC, mm);

            sendMessageNotification(chatRoomId, res, room.getParticipants().stream()
                .map(User::getId).filter(id -> !id.equals(currentUserId)).collect(Collectors.toList()));

            return res;
        } catch (Exception e) {
            throw new RuntimeException("Encrypt message failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decryptMessageForUser(Long messageId, Long userId) {
        ChatMessage msg = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found"));
        try {
            Map<String, String> payload = objectMapper.readValue(msg.getContent(), new TypeReference<>(){});
            String iv = payload.get("iv");
            String data = payload.get("data");
            String kB64 = payload.get("k");
            byte[] keyBytes = Base64.getDecoder().decode(kB64);
            javax.crypto.SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
            var em = new com.nhom4.xoxo.security.EncryptionService.EncryptedMessage();
            em.setIv(iv);
            em.setEncryptedData(data);
            return encryptionService.decryptMessage(em, aesKey);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt message failed: " + e.getMessage(), e);
        }
    }

    // ===== File sharing =====
    @Override
    public FileUploadResponse uploadFileToChat(Long chatRoomId, MultipartFile file, Long currentUserId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new NotFoundException("Chat room not found"));
        User sender = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Sender not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        try {
            String url = cloudinaryService.uploadImageAndGetUrl(file, "chat/" + chatRoomId);
            String mime = file.getContentType();
            long size = file.getSize();

            ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .type(MessageType.FILE)
                .content(file.getOriginalFilename())
                .mediaUrl(url)
                .mediaType(mime)
                .sentAt(LocalDateTime.now())
                .delivered(false)
                .read(false)
                .deleted(false)
                .build();
            msg = chatMessageRepository.save(msg);

            MongoChatMessage mm = MongoChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .content(file.getOriginalFilename())
                .type(MessageType.FILE)
                .mediaUrl(url)
                .mediaType(mime)
                .sentAt(java.time.Instant.now())
                .senderName(getDisplayName(sender))
                .senderAvatar(sender.getAvatarUrl())
                .messageId(msg.getId())
                .build();
            mongoChatMessageRepository.save(mm);

            ChatMessageResponse chatRes = ChatMessageResponse.builder()
                .id(msg.getId())
                .chatRoomId(chatRoomId)
                .senderId(currentUserId)
                .senderName(getDisplayName(sender))
                .senderAvatar(sender.getAvatarUrl())
                .content(file.getOriginalFilename())
                .type(MessageType.FILE)
                .mediaUrl(url)
                .mediaType(mime)
                .sentAt(msg.getSentAt())
                .build();

            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, chatRes);
            kafkaTemplate.send(CHAT_TOPIC, mm);

            sendMessageNotification(chatRoomId, chatRes, room.getParticipants().stream()
                .map(User::getId).filter(id -> !id.equals(currentUserId)).collect(Collectors.toList()));

            return FileUploadResponse.builder()
                .id(msg.getId())
                .fileName(file.getOriginalFilename())
                .originalFileName(file.getOriginalFilename())
                .fileUrl(url)
                .fileSize(size)
                .mimeType(mime)
                .chatMessageId(msg.getId())
                .uploadedBy(currentUserId)
                .uploadedByName(getDisplayName(sender))
                .uploadedAt(msg.getSentAt())
                .encrypted(false)
                .downloadUrl(url)
                .previewUrl(url)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Upload file failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Page<FileUploadResponse> getChatFiles(Long chatRoomId, Long currentUserId, Pageable pageable) {
        Page<ChatMessageResponse> page = baseChatService.getChatMessages(chatRoomId, currentUserId, pageable);
        List<FileUploadResponse> list = page.getContent().stream()
            .filter(m -> m.getType() == MessageType.FILE)
            .map(m -> FileUploadResponse.builder()
                .id(m.getId())
                .fileName(m.getContent())
                .originalFileName(m.getContent())
                .fileUrl(m.getMediaUrl())
                .mimeType(m.getMediaType())
                .chatMessageId(m.getId())
                .uploadedBy(m.getSenderId())
                .uploadedByName(m.getSenderName())
                .uploadedAt(m.getSentAt())
                .downloadUrl(m.getMediaUrl())
                .previewUrl(m.getMediaUrl())
                .build())
            .collect(Collectors.toList());
        return new PageImpl<>(list, pageable, page.getTotalElements());
    }

    @Override
    public byte[] downloadFile(Long fileId, Long currentUserId) {
        ChatMessage msg = chatMessageRepository.findById(fileId)
            .orElseThrow(() -> new NotFoundException("File message not found"));
        if (msg.getMediaUrl() == null) throw new NotFoundException("No media url");
        return new RestTemplate().getForObject(msg.getMediaUrl(), byte[].class);
    }

    @Override
    public void deleteFile(Long fileId, Long currentUserId) {
        ChatMessage msg = chatMessageRepository.findById(fileId)
            .orElseThrow(() -> new NotFoundException("File message not found"));
        if (msg.getMediaUrl() != null) {
            try { cloudinaryService.deleteImage(msg.getMediaUrl()); } catch (Exception e) { log.warn("Cloudinary delete failed: {}", e.getMessage()); }
        }
        msg.setDeleted(true);
        chatMessageRepository.save(msg);
    }

    @Override
    public void shareFileWithUsers(Long fileId, List<Long> userIds, Long currentUserId) {
        ChatMessage msg = chatMessageRepository.findById(fileId)
            .orElseThrow(() -> new NotFoundException("File message not found"));
        ChatMessageResponse res = ChatMessageResponse.builder()
            .id(msg.getId())
            .chatRoomId(msg.getChatRoom().getId())
            .senderId(msg.getSender().getId())
            .senderName(getDisplayName(msg.getSender()))
            .senderAvatar(msg.getSender().getAvatarUrl())
            .content(msg.getContent())
            .type(msg.getType())
            .mediaUrl(msg.getMediaUrl())
            .mediaType(msg.getMediaType())
            .sentAt(msg.getSentAt())
            .build();
        userIds.forEach(uid -> messagingTemplate.convertAndSendToUser(uid.toString(), "/queue/chat/" + res.getChatRoomId(), res));
        if (pushNotificationService != null) {
            PushNotificationRequest req = PushNotificationRequest.builder()
                .title("File shared")
                .body(msg.getContent())
                .notificationData(PushNotificationRequest.NotificationData.builder()
                    .type("CHAT")
                    .chatRoomId(res.getChatRoomId())
                    .senderId(res.getSenderId())
                    .senderName(res.getSenderName())
                    .messagePreview("[File] " + msg.getContent())
                    .messageId(res.getId())
                    .build())
                .build();
            pushNotificationService.sendToUsers(userIds, req);
        }
    }

    // ===== Push helpers =====
    @Override
    public void sendMessageNotification(Long chatRoomId, ChatMessageResponse message, List<Long> recipientIds) {
        if (pushNotificationService == null || recipientIds == null || recipientIds.isEmpty()) return;
        PushNotificationRequest req = PushNotificationRequest.builder()
            .title(message.getSenderName())
            .body(message.getType() == MessageType.FILE ? "[File] " + message.getContent() : message.getContent())
            .notificationData(PushNotificationRequest.NotificationData.builder()
                .type("CHAT")
                .chatRoomId(chatRoomId)
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .messagePreview(message.getContent())
                .messageId(message.getId())
                .build())
            .build();
        pushNotificationService.sendToUsers(recipientIds, req);
    }

    @Override
    public void sendTypingNotification(Long chatRoomId, Long userId, boolean isTyping) {
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/typing", Map.of("userId", userId, "typing", isTyping));
    }

    @Override
    public void sendUserStatusNotification(Long userId, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/status", Map.of("userId", userId, "online", isOnline));
    }

    // ===== Device management =====
    @Override
    public void updateUserDevice(Long userId, String deviceId, String fcmToken, String deviceType, String deviceModel, String operatingSystem, String appVersion) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserDevice device = userDeviceRepository.findByDeviceId(deviceId).orElse(UserDevice.builder().user(user).deviceId(deviceId).build());
        device.setFcmToken(fcmToken);
        device.setDeviceType(deviceType);
        device.setDeviceModel(deviceModel);
        device.setOperatingSystem(operatingSystem);
        device.setAppVersion(appVersion);
        device.setLastSeenAt(LocalDateTime.now());
        device.setActive(true);
        userDeviceRepository.save(device);
    }

    @Override
    public void togglePushNotifications(Long userId, boolean enabled) {
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        devices.forEach(d -> d.setPushEnabled(enabled));
        userDeviceRepository.saveAll(devices);
    }

    // ===== Advanced placeholders =====
    @Override
    public Page<ChatMessageResponse> searchMessages(Long chatRoomId, String query, Long currentUserId, Pageable pageable) {
        Page<ChatMessageResponse> page = baseChatService.getChatMessages(chatRoomId, currentUserId, pageable);
        List<ChatMessageResponse> filtered = page.getContent().stream()
            .filter(m -> Optional.ofNullable(m.getContent()).orElse("").toLowerCase()
                .contains(Optional.ofNullable(query).orElse("").toLowerCase()))
            .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    @Override
    public void pinMessage(Long messageId, Long currentUserId) { log.info("Pin message {} by {}", messageId, currentUserId); }

    @Override
    public void unpinMessage(Long messageId, Long currentUserId) { log.info("Unpin message {} by {}", messageId, currentUserId); }

    @Override
    public List<ChatMessageResponse> getPinnedMessages(Long chatRoomId, Long currentUserId) { return List.of(); }

    @Override
    public void reactToMessage(Long messageId, String reaction, Long currentUserId) { log.info("Reaction {} on {} by {}", reaction, messageId, currentUserId); }

    @Override
    public void removeReaction(Long messageId, String reaction, Long currentUserId) { log.info("Remove reaction {} on {} by {}", reaction, messageId, currentUserId); }

    private String getDisplayName(User user) {
        String first = Optional.ofNullable(user.getFirstName()).orElse("");
        String last = Optional.ofNullable(user.getLastName()).orElse("");
        String full = (first + " " + last).trim();
        return full.isEmpty() ? Optional.ofNullable(user.getUsername()).orElse("User") : full;
    }
}