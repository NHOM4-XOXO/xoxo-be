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
import com.nhom4.xoxo.service.NewsFeedService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service to integrate NewsFeed with other system events
 * Automatically creates feed items when activities happen
 */
@Service
@Slf4j
@Transactional
public class NewsFeedIntegrationService {
    
    private final NewsFeedService newsFeedService;
    private final FriendshipRepository friendshipRepository;
    
    public NewsFeedIntegrationService(NewsFeedService newsFeedService, 
                                    FriendshipRepository friendshipRepository) {
        this.newsFeedService = newsFeedService;
        this.friendshipRepository = friendshipRepository;
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
                
                log.info("Added new post {} to {} friends' feeds", post.getId(), friendIds.size());
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
            }
            
            // Optionally add to mutual friends' feeds for popular posts
            if (post.getLikeCount() != null && post.getLikeCount() > 10) {
                // Get mutual friends and add trending activity
                // TODO: Implement mutual friends logic
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
            
            // Add to mutual friends' feeds
            // TODO: Implement mutual friends notification
            
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
}
