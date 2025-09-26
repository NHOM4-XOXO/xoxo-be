package com.nhom4.xoxo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.NewsFeedItem;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.NewsFeedItemType;

@Repository
public interface NewsFeedItemRepository extends JpaRepository<NewsFeedItem, Long> {
    
    /**
     * Get paginated news feed items for a user (POSTS ONLY), ordered by interaction status, priority score and activity time
     * Non-interacted items first, then interacted items
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user " +
           "AND nfi.itemType = com.nhom4.xoxo.enums.NewsFeedItemType.POST " +
           "AND p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    Page<NewsFeedItem> findByUserOrderByPriorityAndTime(@Param("user") User user, Pageable pageable);
    
    /**
     * Get unseen news feed items for a user (POSTS ONLY)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user AND nfi.isSeen = false " +
           "AND nfi.itemType = com.nhom4.xoxo.enums.NewsFeedItemType.POST " +
           "AND p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    Page<NewsFeedItem> findUnseenByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * Get news feed items by type for a user (only ACTIVE posts)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user AND nfi.itemType = :itemType " +
           "AND (p IS NULL OR p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE) " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    Page<NewsFeedItem> findByUserAndItemType(@Param("user") User user, 
                                           @Param("itemType") NewsFeedItemType itemType, 
                                           Pageable pageable);
    
    /**
     * Get recent news feed items for a user (last 24 hours, only ACTIVE posts)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user AND nfi.activityTime >= :since " +
           "AND (p IS NULL OR p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE) " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    Page<NewsFeedItem> findRecentByUser(@Param("user") User user, 
                                       @Param("since") LocalDateTime since, 
                                       Pageable pageable);
    
    /**
     * Get news feed items from specific actors (friends, groups, only ACTIVE posts)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user AND nfi.actor IN :actors " +
           "AND (p IS NULL OR p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE) " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    Page<NewsFeedItem> findByUserAndActors(@Param("user") User user, 
                                         @Param("actors") List<User> actors, 
                                         Pageable pageable);
    
    /**
     * Count unseen items for a user (only ACTIVE posts)
     */
    @Query("SELECT COUNT(nfi) FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user AND nfi.isSeen = false " +
           "AND (p IS NULL OR p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE)")
    Long countUnseenByUser(@Param("user") User user);
    
    /**
     * Mark items as seen for a user
     */
    @Modifying
    @Query("UPDATE NewsFeedItem nfi SET nfi.isSeen = true " +
           "WHERE nfi.user = :user AND nfi.id IN :itemIds")
    void markAsSeen(@Param("user") User user, @Param("itemIds") List<Long> itemIds);
    
    /**
     * Mark item as interacted
     */
    @Modifying
    @Query("UPDATE NewsFeedItem nfi SET nfi.isInteracted = true " +
           "WHERE nfi.user = :user AND nfi.id = :itemId")
    void markAsInteracted(@Param("user") User user, @Param("itemId") Long itemId);
    
    /**
     * Delete old news feed items for a user (older than specified date)
     */
    @Modifying
    @Query("DELETE FROM NewsFeedItem nfi " +
           "WHERE nfi.user = :user AND nfi.activityTime < :before")
    void deleteOldItemsForUser(@Param("user") User user, @Param("before") LocalDateTime before);
    
    /**
     * Check if a similar feed item already exists
     */
    @Query("SELECT COUNT(nfi) > 0 FROM NewsFeedItem nfi " +
           "WHERE nfi.user = :user AND nfi.actor = :actor AND nfi.itemType = :itemType " +
           "AND nfi.post = :post AND nfi.activityTime >= :since")
    boolean existsSimilarItem(@Param("user") User user, 
                             @Param("actor") User actor,
                             @Param("itemType") NewsFeedItemType itemType,
                             @Param("post") com.nhom4.xoxo.entity.Post post,
                             @Param("since") LocalDateTime since);
    
    /**
     * Find feed items for bulk operations (only ACTIVE posts)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user " +
           "AND (p IS NULL OR p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE) " +
           "ORDER BY nfi.isInteracted ASC, nfi.activityTime DESC")
    List<NewsFeedItem> findByUserOrderByActivityTime(@Param("user") User user);
    
    /**
     * Get feed items by post ID across all users (for updates/deletions)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi WHERE nfi.post.id = :postId")
    List<NewsFeedItem> findByPostId(@Param("postId") Long postId);
    
    /**
     * Delete all feed items for a specific post
     */
    @Modifying
    @Query("DELETE FROM NewsFeedItem nfi WHERE nfi.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
    
    /**
     * Delete all feed items by item type (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM NewsFeedItem nfi WHERE nfi.itemType = :itemType")
    int deleteByItemType(@Param("itemType") NewsFeedItemType itemType);
    
    /**
     * Count items by type for bulk delete
     */
    @Query("SELECT COUNT(nfi) FROM NewsFeedItem nfi WHERE nfi.itemType = :itemType")
    long countByItemType(@Param("itemType") NewsFeedItemType itemType);
    
    /**
     * Quick query for top 20 posts only (for cache HIT optimization)
     */
    @Query("SELECT nfi FROM NewsFeedItem nfi " +
           "LEFT JOIN nfi.post p " +
           "WHERE nfi.user = :user " +
           "AND nfi.itemType = com.nhom4.xoxo.enums.NewsFeedItemType.POST " +
           "AND p.status = com.nhom4.xoxo.enums.PostStatus.ACTIVE " +
           "ORDER BY nfi.isInteracted ASC, nfi.priorityScore DESC, nfi.activityTime DESC")
    List<NewsFeedItem> findTop20PostsByUser(@Param("user") User user);
}

