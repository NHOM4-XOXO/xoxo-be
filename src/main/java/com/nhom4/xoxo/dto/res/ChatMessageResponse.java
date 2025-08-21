package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long id;
    private String content;
    private MessageType type;
    private Long chatRoomId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String mediaUrl;
    private String mediaType;
    private Long replyToMessageId;
    private LocalDateTime sentAt;
    private boolean delivered;
    private boolean read;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
