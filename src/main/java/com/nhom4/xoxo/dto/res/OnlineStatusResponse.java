package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineStatusResponse {
    private Long chatRoomId;
    private List<UserOnlineStatus> onlineUsers;
    private Integer totalOnline;
    private LocalDateTime lastUpdated;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserOnlineStatus {
        private Long userId;
        private String userName;
        private String avatarUrl;
        private boolean isOnline;
        private LocalDateTime lastSeen;
        private String status; // "online", "away", "busy", "offline"
    }
}













