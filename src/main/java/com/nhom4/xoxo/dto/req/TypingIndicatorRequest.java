package com.nhom4.xoxo.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingIndicatorRequest {
    private Long chatRoomId;
    private Long userId;
    private String userName;
    private boolean isTyping;
}


