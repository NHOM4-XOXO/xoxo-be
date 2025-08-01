package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    
    private Long id;
    private String content;
    private PostStatus status;
    private PostType type;
    private String location;
    private String hashtags;
    private boolean isPublic;
    private boolean allowComments;
    private boolean allowLikes;
    private boolean allowShares;
    
    
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer viewCount;
    

    private UserResponse author;
    
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 