package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;
import java.util.List;

import com.nhom4.xoxo.enums.ChatRoomType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRoomResponse {
    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private ChatRoomType type;
    private Long createdBy;
    private List<Long> participantIds;
    private LocalDateTime lastMessageAt;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
