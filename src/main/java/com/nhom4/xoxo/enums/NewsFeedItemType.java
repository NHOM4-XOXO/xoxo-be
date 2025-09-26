package com.nhom4.xoxo.enums;

/**
 * Enum representing different types of news feed items
 * Similar to Facebook's activity types
 */
public enum NewsFeedItemType {
    
    // Post-related activities
    POST("User created a post"),
    SHARED_POST("User shared a post"),
    
    // Reaction activities  
    LIKED_POST("User liked a post"),
    LOVED_POST("User loved a post"),
    REACTED_POST("User reacted to a post"),
    
    // Comment activities
    COMMENTED_POST("User commented on a post"),
    REPLIED_COMMENT("User replied to a comment"),
    
    // Social activities
    NEW_FRIENDSHIP("Users became friends"),
    FRIENDSHIP_ACCEPTED("User accepted friendship request"),
    
    // Group activities
    JOINED_GROUP("User joined a group"),
    CREATED_GROUP("User created a group"),
    GROUP_POST("User posted in a group"),
    
    // Profile activities
    UPDATED_PROFILE("User updated their profile"),
    UPDATED_COVER_PHOTO("User updated their cover photo"),
    UPDATED_AVATAR("User updated their profile picture"),
    
    // Birthday and special events
    BIRTHDAY("User has a birthday today"),
    ANNIVERSARY("User has an anniversary"),
    
    // System activities
    SUGGESTED_FRIENDS("System suggested friends"),
    TRENDING_POST("Trending post in user's network"),
    MEMORIES("User's memories from previous years");
    
    private final String description;
    
    NewsFeedItemType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

