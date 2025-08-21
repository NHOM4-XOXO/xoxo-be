package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageRequest {
    private Long chatRoomId;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String mediaType;
    private Long replyToMessageId;
}
