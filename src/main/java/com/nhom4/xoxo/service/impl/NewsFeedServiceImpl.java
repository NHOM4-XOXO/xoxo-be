package com.nhom4.xoxo.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.dto.res.NewsFeedItemResponse;
import com.nhom4.xoxo.dto.res.NewsFeedResponse;
import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.dto.res.PostWithMediaResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.NewsFeedItem;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.FriendshipStatus;
import com.nhom4.xoxo.enums.NewsFeedItemType;
import com.nhom4.xoxo.repository.FriendshipRepository;
import com.nhom4.xoxo.repository.NewsFeedItemRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.NewsFeedService;
import com.nhom4.xoxo.service.PostService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional("transactionManager")
public class NewsFeedServiceImpl implements NewsFeedService {
    
    private static final String FEED_CACHE_PREFIX = "user_feed:";
    private static final String UNSEEN_COUNT_PREFIX = "unseen_count:";
    private static final int CACHE_TTL_MINUTES = 30;
    // private static final int MAX_FEED_ITEMS_PER_USER = 1000; // Reserved for future use
    private static final int DAYS_TO_KEEP_FEED_ITEMS = 30;
    
    private final NewsFeedItemRepository newsFeedItemRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostService postService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public NewsFeedServiceImpl(NewsFeedItemRepository newsFeedItemRepository,
                             UserRepository userRepository,
                             PostRepository postRepository,
                             FriendshipRepository friendshipRepository,
                             PostService postService,
                             RedisTemplate<String, Object> redisTemplate,
                             ObjectMapper objectMapper) {
        this.newsFeedItemRepository = newsFeedItemRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.friendshipRepository = friendshipRepository;
        this.postService = postService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public NewsFeedResponse getUserNewsFeed(Long userId, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        String cacheKey = FEED_CACHE_PREFIX + userId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        
        try {
            // Try to get FULL cached response first (no SQL at all!)
            NewsFeedResponse cachedResponse = getFullCachedResponse(cacheKey);
            if (cachedResponse != null) {
                cachedResponse.setCacheStatus("HIT");
                cachedResponse.setLoadTimeMs(System.currentTimeMillis() - startTime);
                log.debug("✅ FULL Cache HIT for user feed: {} (NO SQL!)", userId);
                return cachedResponse;
            }
            
            log.debug("❌ Cache MISS - Loading fresh posts for user: {}", userId);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Get posts only (no friendships/groups) - optimized query
            Page<NewsFeedItem> feedItems = newsFeedItemRepository.findByUserOrderByPriorityAndTime(user, pageable);
            
            log.debug("Found {} POST items for user: {}", feedItems.getTotalElements(), userId);
            
            List<NewsFeedItemResponse> itemResponses = convertToResponses(feedItems.getContent());
            Long unseenCount = getUnseenItemsCountFromCache(userId);
            
            NewsFeedResponse response = NewsFeedResponse.builder()
                .items(itemResponses)
                .currentPage(feedItems.getNumber())
                .totalPages(feedItems.getTotalPages())
                .totalElements(feedItems.getTotalElements())
                .pageSize(feedItems.getSize())
                .hasNext(feedItems.hasNext())
                .hasPrevious(feedItems.hasPrevious())
                .unseenCount(unseenCount)
                .isFirstPage(feedItems.isFirst())
                .isLastPage(feedItems.isLast())
                .cacheStatus("MISS")
                .loadTimeMs(System.currentTimeMillis() - startTime)
                .lastUpdated(LocalDateTime.now().toString())
                .build();
            
            // Cache FULL response (convert LocalDateTime to String first)
            cacheFullResponse(cacheKey, response);
            
            log.debug("✅ Cache MISS response for user: {} with {} posts in {}ms", 
                     userId, response.getTotalElements(), response.getLoadTimeMs());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting user news feed for user: {}", userId, e);
            throw new RuntimeException("Failed to get news feed", e);
        }
    }
    
    @Override
    public NewsFeedResponse getUnseenNewsFeed(Long userId, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Page<NewsFeedItem> feedItems = newsFeedItemRepository.findUnseenByUser(user, pageable);
        List<NewsFeedItemResponse> itemResponses = convertToResponses(feedItems.getContent());
        
        return NewsFeedResponse.builder()
            .items(itemResponses)
            .currentPage(feedItems.getNumber())
            .totalPages(feedItems.getTotalPages())
            .totalElements(feedItems.getTotalElements())
            .pageSize(feedItems.getSize())
            .hasNext(feedItems.hasNext())
            .hasPrevious(feedItems.hasPrevious())
            .unseenCount(feedItems.getTotalElements())
            .isFirstPage(feedItems.isFirst())
            .isLastPage(feedItems.isLast())
            .cacheStatus("DIRECT")
            .loadTimeMs(System.currentTimeMillis() - startTime)
            .lastUpdated(LocalDateTime.now().toString())
            .build();
    }
    
    @Override
    public NewsFeedResponse getNewsFeedByType(Long userId, NewsFeedItemType itemType, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Page<NewsFeedItem> feedItems = newsFeedItemRepository.findByUserAndItemType(user, itemType, pageable);
        List<NewsFeedItemResponse> itemResponses = convertToResponses(feedItems.getContent());
        
        return NewsFeedResponse.builder()
            .items(itemResponses)
            .currentPage(feedItems.getNumber())
            .totalPages(feedItems.getTotalPages())
            .totalElements(feedItems.getTotalElements())
            .pageSize(feedItems.getSize())
            .hasNext(feedItems.hasNext())
            .hasPrevious(feedItems.hasPrevious())
            .unseenCount(getUnseenItemsCount(userId))
            .isFirstPage(feedItems.isFirst())
            .isLastPage(feedItems.isLast())
            .cacheStatus("DIRECT")
            .loadTimeMs(System.currentTimeMillis() - startTime)
            .lastUpdated(LocalDateTime.now().toString())
            .build();
    }
    
    @Override
    public NewsFeedResponse getRecentNewsFeed(Long userId, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Page<NewsFeedItem> feedItems = newsFeedItemRepository.findRecentByUser(user, since, pageable);
        List<NewsFeedItemResponse> itemResponses = convertToResponses(feedItems.getContent());
        
        return NewsFeedResponse.builder()
            .items(itemResponses)
            .currentPage(feedItems.getNumber())
            .totalPages(feedItems.getTotalPages())
            .totalElements(feedItems.getTotalElements())
            .pageSize(feedItems.getSize())
            .hasNext(feedItems.hasNext())
            .hasPrevious(feedItems.hasPrevious())
            .unseenCount(getUnseenItemsCount(userId))
            .isFirstPage(feedItems.isFirst())
            .isLastPage(feedItems.isLast())
            .cacheStatus("DIRECT")
            .loadTimeMs(System.currentTimeMillis() - startTime)
            .lastUpdated(LocalDateTime.now().toString())
            .build();
    }
    
    @Override
    @Transactional("transactionManager")
    public void generateNewsFeedForUser(Long userId) {
        log.info("Generating news feed for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        try {
            // Clear existing feed items first
            List<NewsFeedItem> existingItems = newsFeedItemRepository.findByUserOrderByActivityTime(user);
            if (!existingItems.isEmpty()) {
                newsFeedItemRepository.deleteAll(existingItems);
                log.info("Cleared {} existing feed items for user: {}", existingItems.size(), userId);
            }
            
            // Get user's friends
            List<User> friends = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(user) ? 
                     friendship.getUser() : friendship.getFriend())
                .collect(Collectors.toList());
            
            List<NewsFeedItem> feedItems = new ArrayList<>();
            
            // 1. Get posts from friends (last 30 days instead of 7 to capture more data)
            LocalDateTime postsSince = LocalDateTime.now().minusDays(30);
            List<Post> friendsPosts = postRepository.findRecentPostsByUsers(friends, postsSince);
            
            log.info("Found {} posts from {} friends in last 30 days", friendsPosts.size(), friends.size());
            
            for (Post post : friendsPosts) {
                Double priorityScore = calculatePriorityScore(user, post.getAuthor(), 
                    NewsFeedItemType.POST, post, post.getCreatedAt());
                
                NewsFeedItem feedItem = NewsFeedItem.builder()
                    .user(user)
                    .actor(post.getAuthor())
                    .itemType(NewsFeedItemType.POST)
                    .post(post)
                    .priorityScore(priorityScore)
                    .isSeen(false)
                    .isInteracted(false)
                    .activityTime(post.getCreatedAt())
                    .build();
                
                feedItems.add(feedItem);
            }
            
            // 2. Get public posts if user has no friends or few posts from friends
            if (feedItems.size() < 10) {
                log.info("Not enough posts from friends, adding public posts for user: {}", userId);
                
                List<Post> publicPosts = postRepository.findTop20ByIsPublicTrueAndStatusOrderByCreatedAtDesc(
                    com.nhom4.xoxo.enums.PostStatus.ACTIVE);
                
                for (Post post : publicPosts) {
                    // Don't add posts from the user themselves
                    if (post.getAuthor().equals(user)) {
                        continue;
                    }
                    
                    // Don't add duplicate posts
                    boolean alreadyAdded = feedItems.stream()
                        .anyMatch(item -> item.getPost() != null && item.getPost().getId().equals(post.getId()));
                    
                    if (!alreadyAdded) {
                        Double priorityScore = calculatePriorityScore(user, post.getAuthor(), 
                            NewsFeedItemType.POST, post, post.getCreatedAt());
                        
                        // Lower priority for public posts vs friends' posts
                        priorityScore = priorityScore * 0.7;
                        
                        NewsFeedItem feedItem = NewsFeedItem.builder()
                            .user(user)
                            .actor(post.getAuthor())
                            .itemType(NewsFeedItemType.POST)
                            .post(post)
                            .priorityScore(priorityScore)
                            .isSeen(false)
                            .isInteracted(false)
                            .activityTime(post.getCreatedAt())
                            .metadata("{\"source\": \"public_feed\"}")
                            .build();
                        
                        feedItems.add(feedItem);
                    }
                }
            }
            
            // Skip friendship activities - focus on posts only for better performance
            
            // Save all feed items
            if (!feedItems.isEmpty()) {
                newsFeedItemRepository.saveAll(feedItems);
                log.info("Generated {} POSTS for user: {} (friends: {}, public: {})", 
                    feedItems.size(), userId, 
                    feedItems.stream().filter(item -> item.getItemType() == NewsFeedItemType.POST && item.getMetadata() == null).count(),
                    feedItems.stream().filter(item -> item.getMetadata() != null && item.getMetadata().contains("public_feed")).count()
                );
            } else {
                log.warn("No feed items generated for user: {} - consider checking data or adding sample content", userId);
            }
            
            // Clear cache to force refresh
            clearUserFeedCache(userId);
            
        } catch (Exception e) {
            log.error("Error generating news feed for user: {}", userId, e);
            throw new RuntimeException("Failed to generate news feed", e);
        }
    }
    
    @Override
    @Transactional("transactionManager")
    public NewsFeedItem addFeedItem(Long userId, User actor, NewsFeedItemType itemType, 
                                  Post post, Group group, User targetUser, String metadata) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // Check if similar item already exists (to avoid duplicates)
        LocalDateTime recentThreshold = LocalDateTime.now().minusHours(1);
        if (post != null && newsFeedItemRepository.existsSimilarItem(user, actor, itemType, post, recentThreshold)) {
            log.debug("Similar feed item already exists, skipping: user={}, actor={}, type={}", 
                     userId, actor.getId(), itemType);
            return null;
        }
        
        Double priorityScore = calculatePriorityScore(user, actor, itemType, post, LocalDateTime.now());
        
        NewsFeedItem feedItem = NewsFeedItem.builder()
            .user(user)
            .actor(actor)
            .itemType(itemType)
            .post(post)
            .group(group)
            .targetUser(targetUser)
            .priorityScore(priorityScore)
            .isSeen(false)
            .isInteracted(false)
            .activityTime(LocalDateTime.now())
            .metadata(metadata)
            .build();
        
        NewsFeedItem savedItem = newsFeedItemRepository.save(feedItem);
        
        // Update unseen count in cache
        updateUnseenCount(userId);
        
        // Clear user's feed cache to include new item
        clearUserFeedCache(userId);
        
        log.debug("Added feed item: user={}, actor={}, type={}, priority={}", 
                 userId, actor.getId(), itemType, priorityScore);
        
        return savedItem;
    }
    
    @Override
    @Transactional("transactionManager")
    public void addFeedItemToMultipleUsers(List<Long> userIds, User actor, NewsFeedItemType itemType,
                                         Post post, Group group, User targetUser, String metadata) {
        
        log.info("Adding feed item to {} users: type={}, actor={}", userIds.size(), itemType, actor.getId());
        
        List<NewsFeedItem> feedItems = new ArrayList<>();
        LocalDateTime activityTime = LocalDateTime.now();
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.warn("User not found: {}", userId);
                    continue;
                }
                
                // Skip if it's the same user (don't show your own activities)
                if (user.equals(actor)) {
                    continue;
                }
                
                Double priorityScore = calculatePriorityScore(user, actor, itemType, post, activityTime);
                
                NewsFeedItem feedItem = NewsFeedItem.builder()
                    .user(user)
                    .actor(actor)
                    .itemType(itemType)
                    .post(post)
                    .group(group)
                    .targetUser(targetUser)
                    .priorityScore(priorityScore)
                    .isSeen(false)
                    .isInteracted(false)
                    .activityTime(activityTime)
                    .metadata(metadata)
                    .build();
                
                feedItems.add(feedItem);
                
            } catch (Exception e) {
                log.error("Error creating feed item for user: {}", userId, e);
            }
        }
        
        if (!feedItems.isEmpty()) {
            newsFeedItemRepository.saveAll(feedItems);
            
            // Clear cache for all affected users
            for (Long userId : userIds) {
                clearUserFeedCache(userId);
                updateUnseenCount(userId);
            }
            
            log.info("Successfully added {} feed items", feedItems.size());
        }
    }
    
    @Override
    @Transactional("transactionManager")
    public void markItemsAsSeen(Long userId, List<Long> itemIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        newsFeedItemRepository.markAsSeen(user, itemIds);
        
        // Update unseen count cache
        updateUnseenCount(userId);
        
        log.debug("Marked {} items as seen for user: {}", itemIds.size(), userId);
    }
    
    @Override
    @Transactional("transactionManager")
    public void markItemAsInteracted(Long userId, Long itemId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        newsFeedItemRepository.markAsInteracted(user, itemId);
        
        log.debug("Marked item {} as interacted for user: {}", itemId, userId);
    }
    
    @Override
    public Long getUnseenItemsCount(Long userId) {
        return getUnseenItemsCountFromCache(userId);
    }
    
    private Long getUnseenItemsCountFromCache(Long userId) {
        String cacheKey = UNSEEN_COUNT_PREFIX + userId;
        
        try {
            Object cachedCount = redisTemplate.opsForValue().get(cacheKey);
            if (cachedCount != null) {
                log.debug("✅ Cache HIT for unseen count: {}", userId);
                return Long.valueOf(cachedCount.toString());
            }
        } catch (Exception e) {
            log.warn("Error getting unseen count from cache for user: {}", userId, e);
        }
        
        log.debug("❌ Cache MISS for unseen count: {}, loading from DB", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Long count = newsFeedItemRepository.countUnseenByUser(user);
        
        // Cache the count
        try {
            redisTemplate.opsForValue().set(cacheKey, count, Duration.ofMinutes(5));
            log.debug("✅ Cached unseen count for user: {} = {}", userId, count);
        } catch (Exception e) {
            log.warn("Error caching unseen count for user: {}", userId, e);
        }
        
        return count;
    }
    
    @Override
    @CacheEvict(value = "news-feed", key = "#userId + '*'")
    public void refreshUserFeedCache(Long userId) {
        clearUserFeedCache(userId);
        log.debug("Refreshed feed cache for user: {}", userId);
    }
    
    @Override
    public void clearUserFeedCache(Long userId) {
        try {
            String pattern = FEED_CACHE_PREFIX + userId + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            // Also clear unseen count cache
            String unseenCountKey = UNSEEN_COUNT_PREFIX + userId;
            redisTemplate.delete(unseenCountKey);
            
            log.debug("✅ Cleared all feed cache for user: {}", userId);
        } catch (Exception e) {
            log.warn("❌ Error clearing feed cache for user: {}", userId, e);
        }
    }
    
    @Override
    @Transactional("transactionManager")
    public void cleanupOldFeedItems(Long userId, LocalDateTime before) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        newsFeedItemRepository.deleteOldItemsForUser(user, before);
        
        log.debug("Cleaned up old feed items for user: {} before: {}", userId, before);
    }
    
    @Override
    @Transactional("transactionManager")
    public void updateFeedItemsForPost(Long postId, Post updatedPost) {
        List<NewsFeedItem> feedItems = newsFeedItemRepository.findByPostId(postId);
        
        for (NewsFeedItem item : feedItems) {
            // Recalculate priority if needed
            Double newPriority = calculatePriorityScore(item.getUser(), item.getActor(), 
                item.getItemType(), updatedPost, item.getActivityTime());
            item.setPriorityScore(newPriority);
        }
        
        if (!feedItems.isEmpty()) {
            newsFeedItemRepository.saveAll(feedItems);
            
            // Clear cache for affected users
            feedItems.forEach(item -> clearUserFeedCache(item.getUser().getId()));
            
            log.debug("Updated {} feed items for post: {}", feedItems.size(), postId);
        }
    }
    
    @Override
    @Transactional("transactionManager")
    public void deleteFeedItemsForPost(Long postId) {
        List<NewsFeedItem> feedItems = newsFeedItemRepository.findByPostId(postId);
        List<Long> affectedUserIds = feedItems.stream()
            .map(item -> item.getUser().getId())
            .distinct()
            .collect(Collectors.toList());
        
        newsFeedItemRepository.deleteByPostId(postId);
        
        // Clear cache for affected users
        affectedUserIds.forEach(this::clearUserFeedCache);
        
        log.debug("Deleted {} feed items for post: {}", feedItems.size(), postId);
    }
    
    @Override
    public Double calculatePriorityScore(User user, User actor, NewsFeedItemType itemType, 
                                       Post post, LocalDateTime activityTime) {
        double score = 1.0;
        
        // Base score by item type
        switch (itemType) {
            case POST:
            case SHARED_POST:
                score = 5.0;
                break;
            case LIKED_POST:
            case REACTED_POST:
                score = 2.0;
                break;
            case COMMENTED_POST:
                score = 3.0;
                break;
            case NEW_FRIENDSHIP:
                score = 4.0;
                break;
            default:
                score = 2.0;
        }
        
        // Recency factor (more recent = higher score)
        long hoursAgo = ChronoUnit.HOURS.between(activityTime, LocalDateTime.now());
        double recencyFactor = Math.max(0.1, 1.0 - (hoursAgo / 168.0)); // Decay over a week
        score *= recencyFactor;
        
        // Relationship factor (closer relationship = higher score)
        // TODO: Implement relationship strength calculation
        // For now, assume friends have 2x priority
        boolean isFriend = friendshipRepository.findFriendshipBetweenUsers(user, actor).isPresent();
        if (isFriend) {
            score *= 2.0;
        }
        
        // Post engagement factor
        if (post != null) {
            int totalEngagement = post.getLikeCount() + post.getCommentCount() + post.getShareCount();
            double engagementFactor = 1.0 + Math.log10(Math.max(1, totalEngagement));
            score *= engagementFactor;
        }
        
        return Math.min(100.0, Math.max(0.1, score)); // Clamp between 0.1 and 100
    }
    
    @Override
    public void updatePriorityScores(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<NewsFeedItem> feedItems = newsFeedItemRepository.findByUserOrderByActivityTime(user);
        
        for (NewsFeedItem item : feedItems) {
            Double newPriority = calculatePriorityScore(user, item.getActor(), 
                item.getItemType(), item.getPost(), item.getActivityTime());
            item.setPriorityScore(newPriority);
        }
        
        if (!feedItems.isEmpty()) {
            newsFeedItemRepository.saveAll(feedItems);
            clearUserFeedCache(userId);
            log.debug("Updated priority scores for {} feed items for user: {}", feedItems.size(), userId);
        }
    }
    
    @Override
    public Object getFeedAnalytics(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Basic analytics for now
            Long userItems = (long) newsFeedItemRepository.findByUserOrderByActivityTime(user).size();
            Long unseenItems = getUnseenItemsCount(userId);
            
            // Count by item types
            long postItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.POST, Pageable.unpaged()).getTotalElements();
            long likeItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.LIKED_POST, Pageable.unpaged()).getTotalElements();
            long commentItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.COMMENTED_POST, Pageable.unpaged()).getTotalElements();
            long friendshipItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.NEW_FRIENDSHIP, Pageable.unpaged()).getTotalElements();
            
            return Map.of(
                "totalItems", userItems,
                "unseenItems", unseenItems,
                "itemTypes", Map.of(
                    "posts", postItems,
                    "likes", likeItems,
                    "comments", commentItems,
                    "friendships", friendshipItems
                ),
                "lastGenerated", LocalDateTime.now().toString(),
                "cacheStatus", "CALCULATED"
            );
            
        } catch (Exception e) {
            log.error("Error getting feed analytics for user: {}", userId, e);
            return Map.of("error", "Cannot get analytics", "timestamp", LocalDateTime.now().toString());
        }
    }
    
    @Override
    public List<NewsFeedItemResponse> getPopularContent(Long userId, Pageable pageable) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Get friends to find popular content in their network
            List<User> friends = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(user) ? 
                     friendship.getUser() : friendship.getFriend())
                .collect(Collectors.toList());
            
            if (friends.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Find feed items from friends with high engagement (posts with high priority scores)
            List<NewsFeedItem> popularItems = newsFeedItemRepository.findByUserAndActors(user, friends, pageable)
                .getContent()
                .stream()
                .filter(item -> item.getPriorityScore() > 5.0) // High priority items
                .filter(item -> item.getPost() != null) // Only posts
                .filter(item -> item.getPost().getLikeCount() + item.getPost().getCommentCount() > 5) // High engagement
                .sorted((a, b) -> Double.compare(b.getPriorityScore(), a.getPriorityScore()))
                .collect(Collectors.toList());
            
            return convertToResponses(popularItems);
            
        } catch (Exception e) {
            log.error("Error getting popular content for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getTrendingTopics(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Get recent posts from user's network
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<User> friends = friendshipRepository.findByUserAndStatus(user, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> friendship.getFriend().equals(user) ? 
                     friendship.getUser() : friendship.getFriend())
                .collect(Collectors.toList());
            
            if (friends.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Post> recentPosts = postRepository.findRecentPostsByUsers(friends, since);
            
            // Extract hashtags and count frequency
            Map<String, Long> hashtagCounts = recentPosts.stream()
                .filter(post -> post.getHashtags() != null && !post.getHashtags().isEmpty())
                .flatMap(post -> Arrays.stream(post.getHashtags().split("\\s+")))
                .filter(tag -> tag.startsWith("#"))
                .collect(Collectors.groupingBy(
                    hashtag -> hashtag.toLowerCase(),
                    Collectors.counting()
                ));
            
            // Return top trending hashtags
            return hashtagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error getting trending topics for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private List<NewsFeedItemResponse> convertToResponses(List<NewsFeedItem> feedItems) {
        return feedItems.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    private NewsFeedItemResponse convertToResponse(NewsFeedItem item) {
        NewsFeedItemResponse.NewsFeedItemResponseBuilder builder = NewsFeedItemResponse.builder()
            .id(item.getId())
            .itemType(item.getItemType())
            .priorityScore(item.getPriorityScore())
            .isSeen(item.getIsSeen())
            .isInteracted(item.getIsInteracted())
            .activityTime(item.getActivityTime())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .metadata(item.getMetadata())
            .timeAgo(calculateTimeAgo(item.getActivityTime()))
            .canInteract(true); // TODO: Implement proper permission check
        
        // Convert actor
        if (item.getActor() != null) {
            builder.actor(convertUserToResponse(item.getActor()));
        }
        
        // Convert post with media
        if (item.getPost() != null) {
            builder.post(convertPostWithMediaToResponse(item.getPost()));
        }
        
        // Convert group
        if (item.getGroup() != null) {
            builder.group(GroupResponse.builder()
                .id(item.getGroup().getId())
                .title(item.getGroup().getTitle())
                .build());
        }
        
        // Convert target user
        if (item.getTargetUser() != null) {
            builder.targetUser(convertUserToResponse(item.getTargetUser()));
        }
        
        // Generate display text
        builder.displayText(generateDisplayText(item));
        
        return builder.build();
    }
    
    private UserResponse convertUserToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .username(user.getUsername())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }
    
    private PostWithMediaResponse convertPostWithMediaToResponse(Post post) {
        try {
            // Get PostItemResponse
            PostItemResponse postItem = postService.getPostItemById(post.getId()).orElse(null);
            if (postItem == null) {
                // Fallback to manual conversion if PostService fails
                postItem = new PostItemResponse(
                    post.getId(),
                    post.getContent(),
                    post.getStatus(),
                    post.getType(),
                    post.getLocation(),
                    post.getHashtags(),
                    post.isPublic(),
                    post.isAllowComments(),
                    post.isAllowLikes(),
                    post.isAllowShares(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getShareCount(),
                    post.getViewCount(),
                    post.getAuthor() != null ? post.getAuthor().getId() : null,
                    post.getAuthor() != null ? post.getAuthor().getFirstName() : null,
                    post.getAuthor() != null ? post.getAuthor().getLastName() : null,
                    post.getAuthor() != null ? post.getAuthor().getAvatarUrl() : null,
                    post.getCreatedAt(),
                    post.getUpdatedAt()
                );
            }
            
            // Get media using PostService
            List<MediaResponse> mediaResponses = postService.getPostMedia(post.getId()).stream()
                .map(media -> MediaResponse.builder()
                    .id(media.getId())
                    .mediaUrl(media.getMediaUrl())
                    .mediaType(media.getMediaType())
                    .originalFilename(media.getOriginalFilename())
                    .fileSize(media.getFileSize())
                    .uploadedBy(media.getUploadedBy() != null ? 
                        convertUserToResponse(media.getUploadedBy()) : null)
                    .createdAt(media.getCreatedAt())
                    .updatedAt(media.getUpdatedAt())
                    .build())
                .toList();
            
            return PostWithMediaResponse.builder()
                .post(postItem)
                .media(mediaResponses)
                .build();
                
        } catch (Exception e) {
            log.warn("Error converting post with media for post {}: {}", post.getId(), e.getMessage());
            
            // Fallback to basic post without media
            PostItemResponse postItem = new PostItemResponse(
                post.getId(),
                post.getContent(),
                post.getStatus(),
                post.getType(),
                post.getLocation(),
                post.getHashtags(),
                post.isPublic(),
                post.isAllowComments(),
                post.isAllowLikes(),
                post.isAllowShares(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getShareCount(),
                post.getViewCount(),
                post.getAuthor() != null ? post.getAuthor().getId() : null,
                post.getAuthor() != null ? post.getAuthor().getFirstName() : null,
                post.getAuthor() != null ? post.getAuthor().getLastName() : null,
                post.getAuthor() != null ? post.getAuthor().getAvatarUrl() : null,
                post.getCreatedAt(),
                post.getUpdatedAt()
            );
            
            return PostWithMediaResponse.builder()
                .post(postItem)
                .media(new ArrayList<>())
                .build();
        }
    }
    
    private String calculateTimeAgo(LocalDateTime activityTime) {
        if (activityTime == null) return "";
        
        Duration duration = Duration.between(activityTime, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        
        if (minutes < 1) return "vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        if (days < 7) return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        return (days / 30) + " tháng trước";
    }
    
    private String generateDisplayText(NewsFeedItem item) {
        String actorName = item.getActor() != null ? 
            (item.getActor().getFirstName() + " " + item.getActor().getLastName()).trim() : "Ai đó";
        
        switch (item.getItemType()) {
            case POST:
                return actorName + " đã đăng một bài viết mới";
            case SHARED_POST:
                return actorName + " đã chia sẻ một bài viết";
            case LIKED_POST:
                return actorName + " đã thích một bài viết";
            case COMMENTED_POST:
                return actorName + " đã bình luận về một bài viết";
            case NEW_FRIENDSHIP:
                String targetName = item.getTargetUser() != null ? 
                    (item.getTargetUser().getFirstName() + " " + item.getTargetUser().getLastName()).trim() : "ai đó";
                return actorName + " và " + targetName + " đã trở thành bạn bè";
            case JOINED_GROUP:
                String groupName = item.getGroup() != null ? item.getGroup().getTitle() : "một nhóm";
                return actorName + " đã tham gia nhóm " + groupName;
            default:
                return actorName + " có hoạt động mới";
        }
    }
    
    
    private void updateUnseenCount(Long userId) {
        try {
            String cacheKey = UNSEEN_COUNT_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            // Will be recalculated on next access
        } catch (Exception e) {
            log.warn("Error updating unseen count cache for user: {}", userId, e);
        }
    }
    
    // Full response cache with LocalDateTime handling
    private NewsFeedResponse getFullCachedResponse(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // Parse JSON với ObjectMapper đã config JavaTimeModule
                NewsFeedResponse response = objectMapper.readValue(cached.toString(), NewsFeedResponse.class);
                log.debug("✅ Retrieved full cached response for key: {} ({} items)", cacheKey, response.getTotalElements());
                return response;
            }
        } catch (Exception e) {
            log.debug("❌ Error getting full cached response: {} - {}", cacheKey, e.getMessage());
        }
        return null;
    }
    
    private void cacheFullResponse(String cacheKey, NewsFeedResponse response) {
        try {
            // Convert to JSON using ObjectMapper with JavaTimeModule
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(CACHE_TTL_MINUTES));
            log.debug("✅ Cached FULL response for key: {} ({} KB, TTL: {}min)", 
                     cacheKey, json.length() / 1024, CACHE_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("❌ Error caching full response: {} - {}", cacheKey, e.getMessage());
            log.warn("Falling back to no cache for this request");
        }
    }
    
}
