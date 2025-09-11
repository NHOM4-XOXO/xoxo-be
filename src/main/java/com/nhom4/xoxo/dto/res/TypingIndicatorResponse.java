package com.nhom4.xoxo.dto.res;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingIndicatorResponse {
    private Long chatRoomId;
    private List<TypingUser> typingUsers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TypingUser {
        private Long userId;
        private String userName;
        private String avatarUrl;
    }
}






