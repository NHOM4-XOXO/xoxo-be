package com.nhom4.xoxo.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.FriendshipStatus;
import com.nhom4.xoxo.enums.NewsFeedItemType;
import com.nhom4.xoxo.repository.FriendshipRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to integrate NewsFeed with other system events
 * Automatically creates feed items when activities happen
 */
@Service
@Slf4j
@Transactional("transactionManager")
public class NewsFeedIntegrationService {
    
    private final NewsFeedService newsFeedService;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipService friendshipService;
    
    public NewsFeedIntegrationService(NewsFeedService newsFeedService, 
                                    FriendshipRepository friendshipRepository,
                                    FriendshipService friendshipService) {
        this.newsFeedService = newsFeedService;
        this.friendshipRepository = friendshipRepository;
        this.friendshipService = friendshipService;
    }
    
    /**
     * Handle new post creation - add to friends' feeds
     */
    @Async
    public void handleNewPost(Post post) {
        try {
            log.debug("Processing new post for news feed: {}", post.getId());
            
            // Get author's friends
            List<User> friends = friendshipRepository.findByUserAndStatus(post.getAuthor(), FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(post.getAuthor()) ? 
                     friendship.getUser() : friendship.getFriend())
                .toList();
            
            // Add feed item to friends' feeds
            List<Long> friendIds = friends.stream().map(User::getId).toList();
            
            if (!friendIds.isEmpty()) {
                newsFeedService.addFeedItemToMultipleUsers(
                    friendIds, 
                    post.getAuthor(), 
                    NewsFeedItemType.POST, 
                    post, 
                    null, 
                    null, 
                    null
                );
                
                // Clear cache for all affected friends
                for (Long friendId : friendIds) {
                    newsFeedService.clearUserFeedCache(friendId);
                }
                
                log.info("Added new post {} to {} friends' feeds and cleared cache", post.getId(), friendIds.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing new post for news feed: {}", post.getId(), e);
        }
    }
    
    /**
     * Handle post like - add to relevant feeds
     */
    @Async
    public void handlePostLike(Post post, User user) {
        try {
            log.debug("Processing post like for news feed: post={}, user={}", post.getId(), user.getId());
            
            // Add to post author's feed (if not same user)
            if (!post.getAuthor().equals(user)) {
                newsFeedService.addFeedItem(
                    post.getAuthor().getId(), 
                    user, 
                    NewsFeedItemType.LIKED_POST, 
                    post, 
                    null, 
                    null, 
                    null
                );
                
                // Clear cache for post author
                newsFeedService.clearUserFeedCache(post.getAuthor().getId());
            }
            
            // Add to mutual friends' feeds for popular posts
            if (post.getLikeCount() != null && post.getLikeCount() > 10) {
                addToMutualFriendsFeeds(post, user, "liked a popular post");
            }
            
        } catch (Exception e) {
            log.error("Error processing post like for news feed: post={}, user={}", post.getId(), user.getId(), e);
        }
    }
    
    /**
     * Handle post comment - add to relevant feeds
     */
    @Async
    public void handlePostComment(Post post, User user) {
        try {
            log.debug("Processing post comment for news feed: post={}, user={}", post.getId(), user.getId());
            
            // Add to post author's feed (if not same user)
            if (!post.getAuthor().equals(user)) {
                newsFeedService.addFeedItem(
                    post.getAuthor().getId(), 
                    user, 
                    NewsFeedItemType.COMMENTED_POST, 
                    post, 
                    null, 
                    null, 
                    null
                );
                
                // Clear cache for post author
                newsFeedService.clearUserFeedCache(post.getAuthor().getId());
            }
            
        } catch (Exception e) {
            log.error("Error processing post comment for news feed: post={}, user={}", post.getId(), user.getId(), e);
        }
    }
    
    /**
     * Handle new friendship - add to both users' feeds
     */
    @Async
    public void handleNewFriendship(User user1, User user2) {
        try {
            log.debug("Processing new friendship for news feed: user1={}, user2={}", user1.getId(), user2.getId());
            
            // Add to both users' feeds
            newsFeedService.addFeedItem(
                user1.getId(), 
                user2, 
                NewsFeedItemType.NEW_FRIENDSHIP, 
                null, 
                null, 
                user2, 
                null
            );
            
            newsFeedService.addFeedItem(
                user2.getId(), 
                user1, 
                NewsFeedItemType.NEW_FRIENDSHIP, 
                null, 
                null, 
                user1, 
                null
            );
            
            // Clear cache for both users
            newsFeedService.clearUserFeedCache(user1.getId());
            newsFeedService.clearUserFeedCache(user2.getId());
            
            // Add to mutual friends' feeds
            addToMutualFriendsFeeds(user1, user2, "became friends");
            
        } catch (Exception e) {
            log.error("Error processing new friendship for news feed: user1={}, user2={}", 
                     user1.getId(), user2.getId(), e);
        }
    }
    
    /**
     * Handle post deletion - clean up feed items
     */
    @Async
    public void handlePostDeletion(Long postId) {
        try {
            log.debug("Processing post deletion for news feed: {}", postId);
            
            newsFeedService.deleteFeedItemsForPost(postId);
            
            log.info("Cleaned up feed items for deleted post: {}", postId);
            
        } catch (Exception e) {
            log.error("Error processing post deletion for news feed: {}", postId, e);
        }
    }
    
    /**
     * Manual method to call when post is created (until we have proper events)
     */
    public void onPostCreated(Post post) {
        handleNewPost(post);
    }
    
    /**
     * Manual method to call when post is liked
     */
    public void onPostLiked(Post post, User user) {
        handlePostLike(post, user);
    }
    
    /**
     * Manual method to call when post is commented
     */
    public void onPostCommented(Post post, User user) {
        handlePostComment(post, user);
    }
    
    /**
     * Manual method to call when friendship is created
     */
    public void onFriendshipCreated(User user1, User user2) {
        handleNewFriendship(user1, user2);
    }
    
    /**
     * Manual method to call when post is deleted
     */
    public void onPostDeleted(Long postId) {
        handlePostDeletion(postId);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Add activity to mutual friends' feeds
     */
    private void addToMutualFriendsFeeds(Post post, User actor, String activity) {
        try {
            List<User> mutualFriends = friendshipService.getMutualFriends(
                post.getAuthor().getId(), actor.getId());
            
            if (!mutualFriends.isEmpty()) {
                List<Long> mutualFriendIds = mutualFriends.stream()
                    .map(User::getId)
                    .toList();
                
                newsFeedService.addFeedItemToMultipleUsers(
                    mutualFriendIds,
                    actor,
                    NewsFeedItemType.LIKED_POST, // Reuse existing type for trending activity
                    post,
                    null,
                    null,
                    "trending:" + activity
                );
                
                // Clear cache for mutual friends
                for (Long friendId : mutualFriendIds) {
                    newsFeedService.clearUserFeedCache(friendId);
                }
                
                log.debug("Added trending activity to {} mutual friends: {}", 
                         mutualFriendIds.size(), activity);
            }
        } catch (Exception e) {
            log.warn("Error adding to mutual friends feeds: {}", e.getMessage());
        }
    }
    
    /**
     * Add activity to mutual friends' feeds for friendship events
     */
    private void addToMutualFriendsFeeds(User user1, User user2, String activity) {
        try {
            List<User> mutualFriends = friendshipService.getMutualFriends(
                user1.getId(), user2.getId());
            
            if (!mutualFriends.isEmpty()) {
                List<Long> mutualFriendIds = mutualFriends.stream()
                    .map(User::getId)
                    .toList();
                
                // Add to both users' mutual friends
                newsFeedService.addFeedItemToMultipleUsers(
                    mutualFriendIds,
                    user1,
                    NewsFeedItemType.NEW_FRIENDSHIP,
                    null,
                    null,
                    user2,
                    "mutual:" + activity
                );
                
                // Clear cache for mutual friends
                for (Long friendId : mutualFriendIds) {
                    newsFeedService.clearUserFeedCache(friendId);
                }
                
                log.debug("Added friendship activity to {} mutual friends: {}", 
                         mutualFriendIds.size(), activity);
            }
        } catch (Exception e) {
            log.warn("Error adding friendship to mutual friends feeds: {}", e.getMessage());
        }
    }
    
    // ==================== ADVANCED FEATURES ====================
    
    /**
     * Handle post share - add to relevant feeds
     */
    @Async
    public void handlePostShare(Post post, User sharer, String shareContent) {
        try {
            log.debug("Processing post share for news feed: post={}, sharer={}", post.getId(), sharer.getId());
            
            // Add to post author's feed (if not same user)
            if (!post.getAuthor().equals(sharer)) {
                newsFeedService.addFeedItem(
                    post.getAuthor().getId(),
                    sharer,
                    NewsFeedItemType.SHARED_POST,
                    post,
                    null,
                    null,
                    shareContent
                );
                
                newsFeedService.clearUserFeedCache(post.getAuthor().getId());
            }
            
            // Add to sharer's friends' feeds
            List<User> sharerFriends = friendshipRepository.findByUserAndStatus(sharer, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(sharer) ? 
                     friendship.getUser() : friendship.getFriend())
                .toList();
            
            if (!sharerFriends.isEmpty()) {
                List<Long> friendIds = sharerFriends.stream().map(User::getId).toList();
                
                newsFeedService.addFeedItemToMultipleUsers(
                    friendIds,
                    sharer,
                    NewsFeedItemType.SHARED_POST,
                    post,
                    null,
                    null,
                    shareContent
                );
                
                for (Long friendId : friendIds) {
                    newsFeedService.clearUserFeedCache(friendId);
                }
                
                log.info("Added post share {} to {} friends' feeds", post.getId(), friendIds.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing post share for news feed: post={}, sharer={}", 
                     post.getId(), sharer.getId(), e);
        }
    }
    
    /**
     * Handle group join - add to group members' feeds
     */
    @Async
    public void handleGroupJoin(User user, com.nhom4.xoxo.entity.Group group) {
        try {
            log.debug("Processing group join for news feed: user={}, group={}", user.getId(), group.getId());
            
            // Get group members (assuming Group has a getMembers method or similar)
            List<User> groupMembers = new java.util.ArrayList<>(); // TODO: Implement group members retrieval
            
            if (!groupMembers.isEmpty()) {
                List<Long> memberIds = groupMembers.stream()
                    .map(User::getId)
                    .filter(id -> !id.equals(user.getId())) // Exclude the joiner
                    .toList();
                
                newsFeedService.addFeedItemToMultipleUsers(
                    memberIds,
                    user,
                    NewsFeedItemType.GROUP_JOINED,
                    null,
                    group,
                    null,
                    null
                );
                
                for (Long memberId : memberIds) {
                    newsFeedService.clearUserFeedCache(memberId);
                }
                
                log.info("Added group join to {} members' feeds", memberIds.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing group join for news feed: user={}, group={}", 
                     user.getId(), group.getId(), e);
        }
    }
    
    /**
     * Handle user status update - add to friends' feeds
     */
    @Async
    public void handleUserStatusUpdate(User user, String statusUpdate) {
        try {
            log.debug("Processing user status update for news feed: user={}", user.getId());
            
            // Get user's friends
            List<User> friends = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(user) ? 
                     friendship.getUser() : friendship.getFriend())
                .toList();
            
            if (!friends.isEmpty()) {
                List<Long> friendIds = friends.stream().map(User::getId).toList();
                
                newsFeedService.addFeedItemToMultipleUsers(
                    friendIds,
                    user,
                    NewsFeedItemType.STATUS_UPDATE,
                    null,
                    null,
                    null,
                    statusUpdate
                );
                
                for (Long friendId : friendIds) {
                    newsFeedService.clearUserFeedCache(friendId);
                }
                
                log.info("Added status update to {} friends' feeds", friendIds.size());
            }
            
        } catch (Exception e) {
            log.error("Error processing user status update for news feed: user={}", user.getId(), e);
        }
    }
    
    /**
     * Batch process multiple activities (for performance)
     */
    @Async
    public void handleBatchActivities(List<Runnable> activities) {
        try {
            log.debug("Processing {} batch activities", activities.size());
            
            for (Runnable activity : activities) {
                try {
                    activity.run();
                } catch (Exception e) {
                    log.warn("Error in batch activity: {}", e.getMessage());
                }
            }
            
            log.info("Completed batch processing of {} activities", activities.size());
            
        } catch (Exception e) {
            log.error("Error in batch activities processing", e);
        }
    }
    
    /**
     * Clean up old feed items for a user
     */
    @Async
    public void cleanupUserFeed(Long userId, int daysToKeep) {
        try {
            log.debug("Cleaning up old feed items for user: {} (keeping {} days)", userId, daysToKeep);
            
            java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now()
                .minusDays(daysToKeep);
            
            newsFeedService.cleanupOldFeedItems(userId, cutoffDate);
            
            // Clear cache after cleanup
            newsFeedService.clearUserFeedCache(userId);
            
            log.info("Cleaned up old feed items for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Error cleaning up feed for user: {}", userId, e);
        }
    }
    
    /**
     * Get feed analytics for monitoring
     */
    public java.util.Map<String, Object> getFeedAnalytics() {
        try {
            java.util.Map<String, Object> analytics = new java.util.HashMap<>();
            
            // Get total feed items count
            Object feedAnalytics = newsFeedService.getFeedAnalytics(1L); // Use dummy user ID
            long totalFeedItems = 0;
            if (feedAnalytics instanceof java.util.Map) {
                totalFeedItems = ((java.util.Map<?, ?>) feedAnalytics).size();
            }
            
            analytics.put("totalFeedItems", totalFeedItems);
            analytics.put("timestamp", java.time.LocalDateTime.now());
            analytics.put("status", "healthy");
            
            return analytics;
            
        } catch (Exception e) {
            log.error("Error getting feed analytics", e);
            return java.util.Map.of("error", e.getMessage(), "status", "error");
        }
    }
}
