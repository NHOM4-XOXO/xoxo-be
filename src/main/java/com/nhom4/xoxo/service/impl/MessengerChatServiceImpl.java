package com.nhom4.xoxo.service.impl;

import com.nhom4.xoxo.chat.MongoChatMessage;
import com.nhom4.xoxo.chat.MongoChatMessageRepository;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.TypingIndicatorResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.MessengerChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessengerChatServiceImpl implements MessengerChatService {

    private final MongoChatMessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> typingRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private static final String TYPING_KEY_PREFIX = "typing:";
    private static final String ONLINE_KEY_PREFIX = "online:";
    private static final int TYPING_TIMEOUT_SECONDS = 10;

    public MessengerChatServiceImpl(
        MongoChatMessageRepository messageRepository,
        MongoTemplate mongoTemplate,
        RedisTemplate<String, Object> redisTemplate,
        @Qualifier("typingRedisTemplate") RedisTemplate<String, Object> typingRedisTemplate,
        SimpMessagingTemplate messagingTemplate,
        UserRepository userRepository) {
    this.messageRepository = messageRepository;
    this.mongoTemplate = mongoTemplate;
    this.redisTemplate=redisTemplate;
    this.typingRedisTemplate = typingRedisTemplate;
    this.messagingTemplate = messagingTemplate;
    this.userRepository = userRepository;
}
    // ==================== Message Status Management ====================

    @Override
    public void markMessageAsDelivered(String messageId, Long userId) {
        try {
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update()
                .set("deliveredTo." + userId, Instant.now())
                .set("delivered", true)
                .set("deliveredAt", Instant.now());

            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            // Notify sender about delivery
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null && !message.getSenderId().equals(userId)) {
                messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/message-status",
                    Map.of(
                        "messageId", messageId,
                        "status", "DELIVERED",
                        "userId", userId,
                        "timestamp", Instant.now()
                    )
                );
            }
        } catch (Exception e) {
            log.error("Error marking message as delivered: {}", messageId, e);
        }
    }

    @Override
    public void markMessageAsRead(String messageId, Long userId) {
        try {
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update()
                .set("readBy." + userId, Instant.now())
                .set("read", true)
                .set("readAt", Instant.now());

            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            // Notify sender about read receipt
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null && !message.getSenderId().equals(userId)) {
                messagingTemplate.convertAndSendToUser(
                    message.getSenderId().toString(),
                    "/queue/message-status",
                    Map.of(
                        "messageId", messageId,
                        "status", "READ",
                        "userId", userId,
                        "timestamp", Instant.now()
                    )
                );
            }
        } catch (Exception e) {
            log.error("Error marking message as read: {}", messageId, e);
        }
    }

    @Override
    public void markConversationAsRead(Long chatRoomId, Long userId) {
        try {
            Query query = new Query(Criteria.where("chatRoomId").is(chatRoomId)
                .and("senderId").ne(userId)
                .and("readBy." + userId).exists(false));
            
            Update update = new Update()
                .set("readBy." + userId, Instant.now())
                .set("read", true)
                .set("readAt", Instant.now());

            mongoTemplate.updateMulti(query, update, MongoChatMessage.class);
            
            // Notify all participants about read status
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, Map.of(
                "type", "CONVERSATION_READ",
                "userId", userId,
                "chatRoomId", chatRoomId,
                "timestamp", Instant.now()
            ));
            
        } catch (Exception e) {
            log.error("Error marking conversation as read: {}", chatRoomId, e);
        }
    }

    // ==================== Message Reactions ====================

    @Override
    public void addReaction(String messageId, Long userId, String reaction) {
        try {
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update().inc("reactions." + reaction, 1);
            
            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            // Notify all participants
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null) {
                messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                    "type", "REACTION_ADDED",
                    "messageId", messageId,
                    "userId", userId,
                    "reaction", reaction,
                    "timestamp", Instant.now()
                ));
            }
        } catch (Exception e) {
            log.error("Error adding reaction to message: {}", messageId, e);
        }
    }

    @Override
    public void removeReaction(String messageId, Long userId) {
        try {
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null && message.getReactions() != null) {
                // Find and remove user's reaction
                Query query = new Query(Criteria.where("id").is(messageId));
                Update update = new Update();
                
                message.getReactions().forEach((reactionType, count) -> {
                    if (count > 0) {
                        update.inc("reactions." + reactionType, -1);
                    }
                });
                
                mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
                
                messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                    "type", "REACTION_REMOVED",
                    "messageId", messageId,
                    "userId", userId,
                    "timestamp", Instant.now()
                ));
            }
        } catch (Exception e) {
            log.error("Error removing reaction from message: {}", messageId, e);
        }
    }

    @Override
    public List<String> getMessageReactions(String messageId) {
        try {
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null && message.getReactions() != null) {
                return message.getReactions().entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error getting message reactions: {}", messageId, e);
        }
        return new ArrayList<>();
    }

    // ==================== Typing Indicators ====================
    @Override
    public void startTyping(Long chatRoomId, Long userId) {
        try {
            String key = TYPING_KEY_PREFIX + chatRoomId;
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                Map<String, Object> typingInfo = Map.of(
                    "userId", userId,
                    "userName", user.getFirstName() + " " + user.getLastName(),
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                    "timestamp", Instant.now().toEpochMilli()
                );
                
                typingRedisTemplate.opsForHash().put(key, userId.toString(), typingInfo);
                typingRedisTemplate.expire(key, TYPING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                // Notify other participants
                TypingIndicatorResponse response = getTypingUsers(chatRoomId);
                messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/typing", response);
            }
        } catch (Exception e) {
            log.error("Error starting typing indicator: {}", chatRoomId, e);
        }
    }

    @Override
    public void stopTyping(Long chatRoomId, Long userId) {
        try {
            String key = TYPING_KEY_PREFIX + chatRoomId;
            typingRedisTemplate.opsForHash().delete(key, userId.toString());
            
            // Notify other participants
            TypingIndicatorResponse response = getTypingUsers(chatRoomId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId + "/typing", response);
        } catch (Exception e) {
            log.error("Error stopping typing indicator: {}", chatRoomId, e);
        }
    }

    @Override
    public TypingIndicatorResponse getTypingUsers(Long chatRoomId) {
        try {
            String key = TYPING_KEY_PREFIX + chatRoomId;
            Map<Object, Object> typingUsers = typingRedisTemplate.opsForHash().entries(key);
            
            List<TypingIndicatorResponse.TypingUser> users = typingUsers.values().stream()
                .map(obj -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userInfo = (Map<String, Object>) obj;
                    return TypingIndicatorResponse.TypingUser.builder()
                        .userId(((Number) userInfo.get("userId")).longValue())
                        .userName((String) userInfo.get("userName"))
                        .avatarUrl((String) userInfo.get("avatarUrl"))
                        .build();
                })
                .collect(Collectors.toList());
            
            return TypingIndicatorResponse.builder()
                .chatRoomId(chatRoomId)
                .typingUsers(users)
                .build();
        } catch (Exception e) {
            log.error("Error getting typing users: {}", chatRoomId, e);
            return TypingIndicatorResponse.builder()
                .chatRoomId(chatRoomId)
                .typingUsers(new ArrayList<>())
                .build();
        }
    }

    // ==================== Message Search ====================

    @Override
    public Page<ChatMessageResponse> searchMessages(Long chatRoomId, String query, Pageable pageable) {
        try {
            Query mongoQuery = new Query(
                Criteria.where("chatRoomId").is(chatRoomId)
                    .and("deleted").ne(true)
                    .and("searchableContent").regex(query, "i")
            ).with(pageable);
            
            List<MongoChatMessage> messages = mongoTemplate.find(mongoQuery, MongoChatMessage.class);
            return convertToPage(messages, pageable);
        } catch (Exception e) {
            log.error("Error searching messages in room {}: {}", chatRoomId, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<ChatMessageResponse> searchGlobalMessages(Long userId, String query, Pageable pageable) {
        // Implementation for global search across all user's conversations
        return Page.empty(pageable);
    }

    // ==================== Message Management ====================

    @Override
    public ChatMessageResponse editMessage(String messageId, String newContent, Long userId) {
        try {
            MongoChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
            
            if (!message.getSenderId().equals(userId)) {
                throw new ForbiddenException("You can only edit your own messages");
            }
            
            // Check if message is too old to edit (e.g., 24 hours)
            if (message.getSentAt().isBefore(Instant.now().minus(24, ChronoUnit.HOURS))) {
                throw new ForbiddenException("Message is too old to edit");
            }
            
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update()
                .set("content", newContent)
                .set("edited", true)
                .set("editedAt", Instant.now())
                .set("searchableContent", newContent.toLowerCase());
            
            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            // Notify participants about edit
            messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                "type", "MESSAGE_EDITED",
                "messageId", messageId,
                "newContent", newContent,
                "editedAt", Instant.now()
            ));
            
            return convertToResponse(messageRepository.findById(messageId).orElse(message));
            
        } catch (Exception e) {
            log.error("Error editing message: {}", messageId, e);
            throw e;
        }
    }

    @Override
    public void deleteMessage(String messageId, Long userId) {
        try {
            MongoChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
            
            if (!message.getSenderId().equals(userId)) {
                throw new ForbiddenException("You can only delete your own messages");
            }
            
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update()
                .set("deleted", true)
                .set("content", "This message was deleted")
                .unset("attachments")
                .unset("mediaUrl");
            
            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            // Notify participants about deletion
            messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                "type", "MESSAGE_DELETED",
                "messageId", messageId,
                "deletedBy", userId
            ));
            
        } catch (Exception e) {
            log.error("Error deleting message: {}", messageId, e);
            throw e;
        }
    }

    @Override
    public ChatMessageResponse forwardMessage(String messageId, Long targetChatRoomId, Long userId) {
        // Implementation for message forwarding
        return null;
    }

    @Override
    public void pinMessage(String messageId, Long userId) {
        try {
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update().set("pinned", true);
            
            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null) {
                messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                    "type", "MESSAGE_PINNED",
                    "messageId", messageId,
                    "pinnedBy", userId
                ));
            }
        } catch (Exception e) {
            log.error("Error pinning message: {}", messageId, e);
        }
    }

    @Override
    public void unpinMessage(String messageId, Long userId) {
        try {
            Query query = new Query(Criteria.where("id").is(messageId));
            Update update = new Update().set("pinned", false);
            
            mongoTemplate.updateFirst(query, update, MongoChatMessage.class);
            
            MongoChatMessage message = messageRepository.findById(messageId).orElse(null);
            if (message != null) {
                messagingTemplate.convertAndSend("/topic/chat/" + message.getChatRoomId(), Map.of(
                    "type", "MESSAGE_UNPINNED",
                    "messageId", messageId,
                    "unpinnedBy", userId
                ));
            }
        } catch (Exception e) {
            log.error("Error unpinning message: {}", messageId, e);
        }
    }

    @Override
    public List<ChatMessageResponse> getPinnedMessages(Long chatRoomId) {
        try {
            Query query = new Query(
                Criteria.where("chatRoomId").is(chatRoomId)
                    .and("pinned").is(true)
                    .and("deleted").ne(true)
            );
            
            List<MongoChatMessage> messages = mongoTemplate.find(query, MongoChatMessage.class);
            return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting pinned messages: {}", chatRoomId, e);
            return new ArrayList<>();
        }
    }

    // ==================== Advanced Features ====================

    @Override
    public ChatMessageResponse replyToMessage(String originalMessageId, String replyContent, Long userId) {
        // Implementation for message replies
        return null;
    }

    @Override
    public void createMessageThread(String messageId) {
        // Implementation for message threads
    }

    @Override
    public Page<ChatMessageResponse> getMessageThread(String threadId, Pageable pageable) {
        // Implementation for getting thread messages
        return Page.empty(pageable);
    }

    // ==================== Online Status ====================

    @Override
    public void updateUserOnlineStatus(Long userId, boolean isOnline) {
        try {
            String key = ONLINE_KEY_PREFIX + userId;
            if (isOnline) {
                redisTemplate.opsForValue().set(key, Instant.now().toEpochMilli(), 5, TimeUnit.MINUTES);
            } else {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.error("Error updating online status for user: {}", userId, e);
        }
    }

    @Override
    public void updateLastSeen(Long userId) {
        try {
            String key = "last_seen:" + userId;
            redisTemplate.opsForValue().set(key, Instant.now().toEpochMilli());
        } catch (Exception e) {
            log.error("Error updating last seen for user: {}", userId, e);
        }
    }

    @Override
    public List<Long> getOnlineUsers(Long chatRoomId) {
        // Implementation to get online users in a chat room
        return new ArrayList<>();
    }

    // ==================== Message Analytics ====================

    @Override
    public int getUnreadMessageCount(Long userId) {
        try {
            Query query = new Query(
                Criteria.where("senderId").ne(userId)
                    .and("readBy." + userId).exists(false)
                    .and("deleted").ne(true)
            );
            
            return (int) mongoTemplate.count(query, MongoChatMessage.class);
        } catch (Exception e) {
            log.error("Error getting unread message count for user: {}", userId, e);
            return 0;
        }
    }

    @Override
    public int getUnreadMessageCountForRoom(Long chatRoomId, Long userId) {
        try {
            Query query = new Query(
                Criteria.where("chatRoomId").is(chatRoomId)
                    .and("senderId").ne(userId)
                    .and("readBy." + userId).exists(false)
                    .and("deleted").ne(true)
            );
            
            return (int) mongoTemplate.count(query, MongoChatMessage.class);
        } catch (Exception e) {
            log.error("Error getting unread message count for room: {}", chatRoomId, e);
            return 0;
        }
    }

    // ==================== Bulk Operations ====================

    @Override
    public void markMultipleMessagesAsRead(List<String> messageIds, Long userId) {
        try {
            Query query = new Query(Criteria.where("id").in(messageIds));
            Update update = new Update()
                .set("readBy." + userId, Instant.now())
                .set("read", true)
                .set("readAt", Instant.now());
            
            mongoTemplate.updateMulti(query, update, MongoChatMessage.class);
        } catch (Exception e) {
            log.error("Error marking multiple messages as read: {}", e.getMessage(), e);
        }
    }

    @Override
    public void deleteMultipleMessages(List<String> messageIds, Long userId) {
        try {
            Query query = new Query(
                Criteria.where("id").in(messageIds)
                    .and("senderId").is(userId)
            );
            Update update = new Update()
                .set("deleted", true)
                .set("content", "This message was deleted");
            
            mongoTemplate.updateMulti(query, update, MongoChatMessage.class);
        } catch (Exception e) {
            log.error("Error deleting multiple messages: {}", e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    private Page<ChatMessageResponse> convertToPage(List<MongoChatMessage> messages, Pageable pageable) {
        List<ChatMessageResponse> responses = messages.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(responses, pageable, responses.size());
    }

    private ChatMessageResponse convertToResponse(MongoChatMessage message) {
        return ChatMessageResponse.builder()
            .id(message.getMessageId())
            .content(message.getContent())
            .type(message.getType())
            .chatRoomId(message.getChatRoomId())
            .senderId(message.getSenderId())
            .senderName(message.getSenderName())
            .senderAvatar(message.getSenderAvatar())
            .mediaUrl(message.getMediaUrl())
            .mediaType(message.getMediaType())
            .sentAt(java.time.LocalDateTime.ofInstant(message.getSentAt(), java.time.ZoneId.systemDefault()))
            .delivered(message.isDelivered())
            .read(message.isRead())
            .deliveredAt(message.getDeliveredAt() != null ? 
                java.time.LocalDateTime.ofInstant(message.getDeliveredAt(), java.time.ZoneId.systemDefault()) : null)
            .readAt(message.getReadAt() != null ? 
                java.time.LocalDateTime.ofInstant(message.getReadAt(), java.time.ZoneId.systemDefault()) : null)
            .replyToMessageId(message.getReplyToMessageId())
            .deleted(message.isDeleted())
            .edited(message.isEdited())
            .editedAt(message.getEditedAt() != null ? 
                java.time.LocalDateTime.ofInstant(message.getEditedAt(), java.time.ZoneId.systemDefault()) : null)
            .pinned(message.isPinned())
            .reactions(message.getReactions() != null ? message.getReactions() : new java.util.HashMap<>())
            .mentionedUserIds(message.getMentionedUserIds() != null ? message.getMentionedUserIds() : new java.util.ArrayList<>())
            .build();
    }
}
