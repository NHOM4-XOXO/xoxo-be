package com.nhom4.xoxo.dto.res;

import com.nhom4.xoxo.enums.PostReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostReactionStatsResponse {
    private Long postId;
    private Long totalReactions;
    private Map<PostReactionType, Long> reactionCounts;
    private PostReactionType topReaction;
    private Long topReactionCount;
    
    // Top reactors
    private java.util.List<TopReactor> topReactors;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TopReactor {
        private Long userId;
        private String userName;
        private String userAvatar;
        private PostReactionType reactionType;
        private String emoji;
    }
}







