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
    
    private boolean delivered = false;
    private boolean read = false;
    private Instant deliveredAt;
    private Instant readAt;
    private boolean deleted = false;
    
    // Metadata
    private String senderName;
    private String senderAvatar;
    private Long messageId; // Reference to MySQL message
}
