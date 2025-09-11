package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {
    
    private List<UserSearchItem> users;
    private List<PostSearchItem> posts;
    private List<GroupSearchItem> groups;
    
    private int totalUsers;
    private int totalPosts;
    private int totalGroups;
    private int totalResults;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSearchItem {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String avatarUrl;
        private String bio;
       
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostSearchItem {
        private Long id;
        private String content;
        private String authorName;
        private String authorAvatarUrl;
        private Long authorId;
        private String location;
        private String hashtags;
        private int likeCount;
        private int commentCount;
        private int shareCount;
        private String createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSearchItem {
        private Long id;
        private String title;
        private String description;
        private String coverUrl;
        private String creatorName;
        private String privacy;
        private int memberCount;
        private int postCount;
       
        private String location;
        private String tags;
    }
}
