package com.nhom4.xoxo.dto.res;

import com.nhom4.xoxo.enums.PostReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostReactionResponse {
    private Long id;
    private Long postId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private PostReactionType reactionType;
    private String emoji;
    private String displayName;
    private LocalDateTime createdAt;
}







