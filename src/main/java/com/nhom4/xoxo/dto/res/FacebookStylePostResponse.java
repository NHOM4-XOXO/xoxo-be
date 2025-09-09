package com.nhom4.xoxo.dto.res;

import com.nhom4.xoxo.enums.PostReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacebookStylePostResponse {
    // Basic post info
    private Long id;
    private String content;
    private UserResponse author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Media
    private List<MediaResponse> media;
    
    // Facebook-style engagement data
    private EngagementData engagement;
    
    // User's interaction with this post
    private UserInteraction userInteraction;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EngagementData {
        // Reaction data
        private Integer totalReactions;
        private Map<PostReactionType, Long> reactionBreakdown;
        private String reactionSummary; // "👍 45, ❤️ 32, 😂 25"
        private List<TopReaction> topReactions; // Top 3 reactions
        
        // Other engagement
        private Integer commentCount;
        private Integer shareCount;
        private Integer viewCount;
        
        // Recent reactors (for "John and 42 others" display)
        private List<RecentReactor> recentReactors;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInteraction {
        private PostReactionType userReaction; // User's current reaction
        private boolean hasLiked;
        private boolean hasCommented;
        private boolean hasShared;
        private boolean hasViewed;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TopReaction {
        private PostReactionType type;
        private String emoji;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RecentReactor {
        private Long userId;
        private String userName;
        private String userAvatar;
        private PostReactionType reactionType;
        private String emoji;
        private LocalDateTime reactedAt;
    }
}




