package com.nhom4.xoxo.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.chat.MongoChatMessage;
import com.nhom4.xoxo.chat.MongoChatMessageRepository;
import com.nhom4.xoxo.dto.req.ChatMessageRequest;
import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.ChatRoomResponse;
import com.nhom4.xoxo.entity.ChatMessage;
import com.nhom4.xoxo.entity.ChatParticipant;
import com.nhom4.xoxo.entity.ChatRoom;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.ChatRoomType;
import com.nhom4.xoxo.enums.MessageType;
import com.nhom4.xoxo.enums.ParticipantStatus;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.ChatMessageRepository;
import com.nhom4.xoxo.repository.ChatParticipantRepository;
import com.nhom4.xoxo.repository.ChatRoomRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
@Transactional("transactionManager")
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;
    private final MongoChatMessageRepository mongoChatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CHAT_TOPIC = "chat-messages";

    @Override
    public ChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ChatRoom chatRoom = ChatRoom.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .createdBy(currentUserId)
                .lastMessageAt(LocalDateTime.now())
                .active(true)
                .build();

        // Add participants
        Set<User> participants = new HashSet<>();
        participants.add(currentUser);

        if (request.getParticipantIds() != null) {
            for (Long participantId : request.getParticipantIds()) {
                if (!participantId.equals(currentUserId)) {
                    User participant = userRepository.findById(participantId)
                            .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
                    participants.add(participant);
                }
            }
        }

        chatRoom.setParticipants(participants);
        chatRoom = chatRoomRepository.save(chatRoom);

        // Create participant records
        for (User participant : participants) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoom(chatRoom)
                    .user(participant)
                    .status(ParticipantStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .isAdmin(participant.getId().equals(currentUserId))
                    .active(true)
                    .build();
            chatParticipantRepository.save(chatParticipant);
        }

        return mapToChatRoomResponse(chatRoom);
    }

    @Override
    public ChatRoomResponse getChatRoomById(Long chatRoomId, Long currentUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        // Check if user is participant
        if (!isUserParticipant(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Access denied to chat room");
        }

        return mapToChatRoomResponse(chatRoom);
    }

    @Override
    public List<ChatRoomResponse> getUserChatRooms(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUserIdWithMessages(userId);

        return chatRooms.stream()
                .map(chatRoom -> {
                    // Bây giờ messages đã được load sẵn
                    String lastMessage = getLastMessageContentSafely(chatRoom);
                    LocalDateTime lastMessageAt = getLastMessageTimeSafely(chatRoom);

                    return ChatRoomResponse.builder()
                            .id(chatRoom.getId())
                            .name(chatRoom.getName())
                            .description(chatRoom.getDescription())
                            .avatarUrl(chatRoom.getAvatarUrl())
                            .type(chatRoom.getType())
                            .createdBy(chatRoom.getCreatedBy())
                            .participantIds(chatRoom.getParticipants().stream()
                                    .map(User::getId)
                                    .collect(Collectors.toList()))
                            .lastMessage(lastMessage)
                            .lastMessageAt(lastMessageAt)
                            .active(chatRoom.isActive())
                            .createdAt(chatRoom.getCreatedAt())
                            .updatedAt(chatRoom.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Sửa method để dùng query riêng
    private String getLastMessageContentSafely(ChatRoom chatRoom) {
        try {
            Optional<ChatMessage> lastMessage = chatRoomRepository.findLastMessageByChatRoomId(chatRoom.getId());
            if (lastMessage.isPresent()) {
                System.out.println(
                        "Found last message for ChatRoom " + chatRoom.getId() + ": " + lastMessage.get().getContent());
                return lastMessage.get().getContent();
            } else {
                System.out.println("No last message found for ChatRoom " + chatRoom.getId() + ", using fallback: "
                        + chatRoom.getLastMessage());
                return chatRoom.getLastMessage();
            }
        } catch (Exception e) {
            System.out.println(
                    "Error getting last message for ChatRoom ID: " + chatRoom.getId() + ", Error: " + e.getMessage());
            return chatRoom.getLastMessage();
        }
    }

    private LocalDateTime getLastMessageTimeSafely(ChatRoom chatRoom) {
        try {
            Optional<ChatMessage> lastMessage = chatRoomRepository.findLastMessageByChatRoomId(chatRoom.getId());
            if (lastMessage.isPresent()) {
                return lastMessage.get().getSentAt();
            } else {
                return chatRoom.getLastMessageAt();
            }
        } catch (Exception e) {
            return chatRoom.getLastMessageAt();
        }
    }

    @Override
    public ChatRoomResponse updateChatRoom(Long chatRoomId, CreateChatRoomRequest request, Long currentUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        // Check if user is admin
        if (!isUserAdmin(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Only admin can update chat room");
        }

        chatRoom.setName(request.getName());
        chatRoom.setDescription(request.getDescription());
        chatRoom = chatRoomRepository.save(chatRoom);

        return mapToChatRoomResponse(chatRoom);
    }

    @Override
    public void deleteChatRoom(Long chatRoomId, Long currentUserId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        // Check if user is admin
        if (!isUserAdmin(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Only admin can delete chat room");
        }

        chatRoom.setActive(false);
        chatRoomRepository.save(chatRoom);
    }

    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        // Check if user is participant
        if (!isUserParticipant(request.getChatRoomId(), currentUserId)) {
            throw new ForbiddenException("Access denied to chat room");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .content(request.getContent())
                .type(request.getType())
                .chatRoom(chatRoom)
                .sender(currentUser)
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .replyToMessageId(request.getReplyToMessageId())
                .sentAt(LocalDateTime.now())
                .delivered(false)
                .read(false)
                .deleted(false)
                .build();

        chatMessage = chatMessageRepository.save(chatMessage);

        // Update last message time
        chatRoom.setLastMessageAt(LocalDateTime.now());
        chatRoom.setLastMessage(request.getContent());
        chatRoomRepository.save(chatRoom);

        // Save to MongoDB for real-time access
        MongoChatMessage mongoMessage = MongoChatMessage.builder()
                .chatRoomId(chatRoom.getId())
                .senderId(currentUser.getId())
                .content(request.getContent())
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .mediaType(request.getMediaType())
                .replyToMessageId(request.getReplyToMessageId())
                .sentAt(java.time.Instant.now())
                .delivered(false)
                .read(false)
                .deleted(false)
                .senderName(currentUser.getFirstName() + " " + currentUser.getLastName())
                .senderAvatar(currentUser.getAvatarUrl())
                .messageId(chatMessage.getId())
                .build();

        mongoChatMessageRepository.save(mongoMessage);

        // Send to Kafka for processing
        kafkaTemplate.send(CHAT_TOPIC, mongoMessage);

        // Send real-time notification to participants
        sendRealTimeMessage(chatRoom.getId(), mapToChatMessageResponse(chatMessage));

        return mapToChatMessageResponse(chatMessage);
    }

    @Override
    public Page<ChatMessageResponse> getChatMessages(Long chatRoomId, Long currentUserId, Pageable pageable) {
        // Check if user is participant
        if (!isUserParticipant(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Access denied to chat room");
        }

        Page<ChatMessage> messages = chatMessageRepository.findMessagesByChatRoomId(chatRoomId, pageable);
        return messages.map(this::mapToChatMessageResponse);
    }

    @Override
    public ChatMessageResponse getMessageById(Long messageId, Long currentUserId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        // Check if user is participant
        if (!isUserParticipant(message.getChatRoom().getId(), currentUserId)) {
            throw new ForbiddenException("Access denied to message");
        }

        return mapToChatMessageResponse(message);
    }

    @Override
    public void deleteMessage(Long messageId, Long currentUserId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        // Check if user is sender or admin
        if (!message.getSender().getId().equals(currentUserId)
                && !isUserAdmin(message.getChatRoom().getId(), currentUserId)) {
            throw new ForbiddenException("Cannot delete this message");
        }

        message.setDeleted(true);
        chatMessageRepository.save(message);

        // Update MongoDB
        mongoChatMessageRepository.findByChatRoomIdOrderBySentAtDesc(message.getChatRoom().getId(), null)
                .stream()
                .filter(m -> m.getMessageId().equals(messageId))
                .findFirst()
                .ifPresent(m -> {
                    m.setDeleted(true);
                    mongoChatMessageRepository.save(m);
                });
    }

    @Override
    public void addParticipant(Long chatRoomId, Long userId, Long currentUserId) {
        // Check if user is admin
        if (!isUserAdmin(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Only admin can add participants");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException("Chat room not found"));

        // Check if user is already participant
        if (isUserParticipant(chatRoomId, userId)) {
            throw new ForbiddenException("User is already a participant");
        }

        // Add to participants
        chatRoom.getParticipants().add(user);
        chatRoomRepository.save(chatRoom);

        // Create participant record
        ChatParticipant chatParticipant = ChatParticipant.builder()
                .chatRoom(chatRoom)
                .user(user)
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .isAdmin(false)
                .active(true)
                .build();
        chatParticipantRepository.save(chatParticipant);
    }

    @Override
    public void removeParticipant(Long chatRoomId, Long userId, Long currentUserId) {
        // Check if user is admin
        if (!isUserAdmin(chatRoomId, currentUserId)) {
            throw new ForbiddenException("Only admin can remove participants");
        }

        ChatParticipant participant = chatParticipantRepository.findByUserAndChatRoom(userId, chatRoomId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        participant.setStatus(ParticipantStatus.REMOVED);
        participant.setActive(false);
        participant.setLeftAt(LocalDateTime.now());
        chatParticipantRepository.save(participant);
    }

    @Override
    public void leaveChatRoom(Long chatRoomId, Long currentUserId) {
        ChatParticipant participant = chatParticipantRepository.findByUserAndChatRoom(currentUserId, chatRoomId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        participant.setStatus(ParticipantStatus.LEFT);
        participant.setActive(false);
        participant.setLeftAt(LocalDateTime.now());
        chatParticipantRepository.save(participant);
    }

    @Override
    public void markMessageAsRead(Long messageId, Long currentUserId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        // Check if user is participant
        if (!isUserParticipant(message.getChatRoom().getId(), currentUserId)) {
            throw new ForbiddenException("Access denied to message");
        }

        // Only mark as read if user is not the sender
        if (!message.getSender().getId().equals(currentUserId)) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            chatMessageRepository.save(message);

            // Update MongoDB
            mongoChatMessageRepository.findByChatRoomIdOrderBySentAtDesc(message.getChatRoom().getId(), null)
                    .stream()
                    .filter(m -> m.getMessageId().equals(messageId))
                    .findFirst()
                    .ifPresent(m -> {
                        m.setRead(true);
                        m.setReadAt(java.time.Instant.now());
                        mongoChatMessageRepository.save(m);
                    });
        }
    }

    @Override
    public void markMessageAsDelivered(Long messageId, Long currentUserId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        // Check if user is participant
        if (!isUserParticipant(message.getChatRoom().getId(), currentUserId)) {
            throw new ForbiddenException("Access denied to message");
        }

        // Only mark as delivered if user is not the sender
        if (!message.getSender().getId().equals(currentUserId)) {
            message.setDelivered(true);
            message.setDeliveredAt(LocalDateTime.now());
            chatMessageRepository.save(message);

            // Update MongoDB
            mongoChatMessageRepository.findByChatRoomIdOrderBySentAtDesc(message.getChatRoom().getId(), null)
                    .stream()
                    .filter(m -> m.getMessageId().equals(messageId))
                    .findFirst()
                    .ifPresent(m -> {
                        m.setDelivered(true);
                        m.setDeliveredAt(java.time.Instant.now());
                        mongoChatMessageRepository.save(m);
                    });
        }
    }

    @Override
    public Long getUnreadMessageCount(Long chatRoomId, Long currentUserId) {
        return chatMessageRepository.countUnreadMessages(chatRoomId, currentUserId);
    }

    @Override
    public ChatRoomResponse getOrCreateDirectChat(Long otherUserId, Long currentUserId) {
        // Check if direct chat already exists
        Optional<ChatRoom> existingChat = chatRoomRepository.findDirectChatRoom(currentUserId, otherUserId);
        if (existingChat.isPresent()) {
            return mapToChatRoomResponse(existingChat.get());
        }

        // Create new direct chat
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new NotFoundException("Other user not found"));

        ChatRoom directChat = ChatRoom.builder()
                .name(otherUser.getFirstName() + " " + otherUser.getLastName())
                .type(ChatRoomType.DIRECT)
                .createdBy(currentUserId)
                .active(true)
                .build();

        Set<User> participants = new HashSet<>();
        participants.add(currentUser);
        participants.add(otherUser);
        directChat.setParticipants(participants);

        directChat = chatRoomRepository.save(directChat);

        // Create participant records
        for (User participant : participants) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoom(directChat)
                    .user(participant)
                    .status(ParticipantStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .lastSeenAt(LocalDateTime.now())
                    .isAdmin(participant.getId().equals(currentUserId))
                    .active(true)
                    .build();
            chatParticipantRepository.save(chatParticipant);
        }

        return mapToChatRoomResponse(directChat);
    }

    // Helper methods
    private boolean isUserParticipant(Long chatRoomId, Long userId) {
        return chatParticipantRepository.findByUserAndChatRoom(userId, chatRoomId)
                .map(ChatParticipant::isActive)
                .orElse(false);
    }

    private boolean isUserAdmin(Long chatRoomId, Long userId) {
        return chatParticipantRepository.findByUserAndChatRoom(userId, chatRoomId)
                .map(ChatParticipant::isAdmin)
                .orElse(false);
    }

    private void sendRealTimeMessage(Long chatRoomId, ChatMessageResponse message) {
        // Send to all participants in the chat room
        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, message);

        // Send to specific user queues for notifications
        List<ChatParticipant> participants = chatParticipantRepository.findActiveParticipantsByChatRoom(chatRoomId);
        for (ChatParticipant participant : participants) {
            if (!participant.getUser().getId().equals(message.getSenderId())) {
                messagingTemplate.convertAndSendToUser(
                        participant.getUser().getId().toString(),
                        "/queue/chat/" + chatRoomId,
                        message);
            }
        }
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .description(chatRoom.getDescription())
                .avatarUrl(chatRoom.getAvatarUrl())
                .type(chatRoom.getType())
                .createdBy(chatRoom.getCreatedBy())
                .participantIds(chatRoom.getParticipants().stream()
                        .map(User::getId)
                        .collect(Collectors.toList()))
                .lastMessageAt(chatRoom.getLastMessageAt())
                .active(chatRoom.isActive())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .senderAvatar(message.getSender().getAvatarUrl())
                .mediaUrl(message.getMediaUrl())
                .mediaType(message.getMediaType())
                .replyToMessageId(message.getReplyToMessageId())
                .sentAt(message.getSentAt())
                .delivered(message.isDelivered())
                .read(message.isRead())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .build();
    }
}
