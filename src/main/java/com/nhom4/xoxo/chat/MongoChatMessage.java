package com.nhom4.xoxo.chat;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import com.nhom4.xoxo.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "chat_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MongoChatMessage {
    @Id
    private String id;
    
    @Indexed
    private Long chatRoomId;
    
    @Indexed
    private Long senderId;
    
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String mediaType;
    private Long replyToMessageId;
    
    @Indexed
    private Instant sentAt = Instant.now();
    
    // Message Status - Messenger-like
    private boolean delivered = false;
    private boolean read = false;
    private Instant deliveredAt;
    private Instant readAt;
    private boolean deleted = false;
    private boolean edited = false;
    private Instant editedAt;
    
    // Metadata
    private String senderName;
    private String senderAvatar;
    private Long messageId; // Reference to MySQL message
    
    // Messenger-like features
    private java.util.Map<String, Integer> reactions; // reaction -> count
    private java.util.List<String> mentionedUserIds; // @mentions
    private java.util.Map<String, Object> attachments; // files, images, etc.
    private String threadId; // for message threads
    private boolean forwarded = false;
    private String originalMessageId; // if forwarded
    
    // Read receipts - who read this message
    private java.util.Map<String, Instant> readBy; // userId -> readTime
    private java.util.Map<String, Instant> deliveredTo; // userId -> deliveryTime
    
    // Message priority and importance
    private boolean important = false;
    private boolean pinned = false;
    
    // Search optimization
    @org.springframework.data.mongodb.core.index.TextIndexed
    private String searchableContent;
}
