package com.nhom4.xoxo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.nhom4.xoxo.dto.res.NewsFeedItemResponse;
import com.nhom4.xoxo.dto.res.NewsFeedResponse;
import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.NewsFeedItem;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.NewsFeedItemType;

/**
 * Service for managing news feed functionality
 * Handles feed generation, caching, and user interactions
 */
public interface NewsFeedService {
    
    // ==================== FEED RETRIEVAL ====================
    
    /**
     * Get paginated news feed for a user with Redis caching
     */
    NewsFeedResponse getUserNewsFeed(Long userId, Pageable pageable);
    
    /**
     * Get unseen news feed items for a user
     */
    NewsFeedResponse getUnseenNewsFeed(Long userId, Pageable pageable);
    
    /**
     * Get news feed by type (posts, friendships, etc.)
     */
    NewsFeedResponse getNewsFeedByType(Long userId, NewsFeedItemType itemType, Pageable pageable);
    
    /**
     * Get recent news feed items (last 24 hours)
     */
    NewsFeedResponse getRecentNewsFeed(Long userId, Pageable pageable);
    
    // ==================== FEED GENERATION ====================
    
    /**
     * Generate/refresh complete news feed for a user
     * Called when user logs in or periodically
     */
    void generateNewsFeedForUser(Long userId);
    
    /**
     * Add single feed item to user's feed
     */
    NewsFeedItem addFeedItem(Long userId, User actor, NewsFeedItemType itemType, 
                           Post post, Group group, User targetUser, String metadata);
    
    /**
     * Bulk add feed items to multiple users (e.g., when user posts)
     */
    void addFeedItemToMultipleUsers(List<Long> userIds, User actor, NewsFeedItemType itemType,
                                  Post post, Group group, User targetUser, String metadata);
    
    // ==================== FEED INTERACTIONS ====================
    
    /**
     * Mark feed items as seen
     */
    void markItemsAsSeen(Long userId, List<Long> itemIds);
    
    /**
     * Mark feed item as interacted (clicked, liked, etc.)
     */
    void markItemAsInteracted(Long userId, Long itemId);
    
    /**
     * Get unseen items count for a user
     */
    Long getUnseenItemsCount(Long userId);
    
    // ==================== FEED MANAGEMENT ====================
    
    /**
     * Refresh cache for user's news feed
     */
    void refreshUserFeedCache(Long userId);
    
    /**
     * Clear cache for user's news feed
     */
    void clearUserFeedCache(Long userId);
    
    /**
     * Clean up old feed items for a user
     */
    void cleanupOldFeedItems(Long userId, LocalDateTime before);
    
    /**
     * Update feed items when post is updated/deleted
     */
    void updateFeedItemsForPost(Long postId, Post updatedPost);
    
    /**
     * Delete feed items when post is deleted
     */
    void deleteFeedItemsForPost(Long postId);
    
    // ==================== PRIORITY CALCULATION ====================
    
    /**
     * Calculate priority score for a feed item
     * Based on: recency, relationship strength, user interaction history
     */
    Double calculatePriorityScore(User user, User actor, NewsFeedItemType itemType, 
                                Post post, LocalDateTime activityTime);
    
    /**
     * Update priority scores for existing feed items
     */
    void updatePriorityScores(Long userId);
    
    // ==================== ANALYTICS & INSIGHTS ====================
    
    /**
     * Get feed analytics for a user
     */
    Object getFeedAnalytics(Long userId);
    
    /**
     * Get popular content in user's network
     */
    List<NewsFeedItemResponse> getPopularContent(Long userId, Pageable pageable);
    
    /**
     * Get trending topics in user's network
     */
    List<String> getTrendingTopics(Long userId);
}
