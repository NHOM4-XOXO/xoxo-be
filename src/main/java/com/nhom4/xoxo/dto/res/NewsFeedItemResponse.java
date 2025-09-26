package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.NewsFeedItemType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for NewsFeed items
 * Contains all information needed to display a feed item to the user
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsFeedItemResponse {
    
    private Long id;
    private NewsFeedItemType itemType;
    private UserResponse actor;
    private PostWithMediaResponse post;
    private GroupResponse group;
    private UserResponse targetUser;
    private Double priorityScore;
    private Boolean isSeen;
    private Boolean isInteracted;
    private LocalDateTime activityTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata;
    
    // Additional computed fields for better UX
    private String displayText;
    private String timeAgo;
    private Boolean canInteract;
    private Integer interactionCount;
}
