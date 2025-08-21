package com.nhom4.xoxo.dto.req;

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
public class CreateChatRoomRequest {
    private String name;
    private String description;
    private ChatRoomType type;
    private List<Long> participantIds;
}
