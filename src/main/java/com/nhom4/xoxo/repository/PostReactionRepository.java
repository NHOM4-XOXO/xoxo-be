package com.nhom4.xoxo.repository;

import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.PostReaction;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    
    // Basic queries
    Optional<PostReaction> findByPostAndUser(Post post, User user);
    List<PostReaction> findByPost(Post post);
    Page<PostReaction> findByPostOrderByCreatedAtDesc(Post post, Pageable pageable);
    
    // Count queries for Facebook-style analytics
    @Query("SELECT COUNT(pr) FROM PostReaction pr WHERE pr.post = :post")
    long countByPost(@Param("post") Post post);
    
    @Query("SELECT COUNT(pr) FROM PostReaction pr WHERE pr.post = :post AND pr.reactionType = :reactionType")
    long countByPostAndReactionType(@Param("post") Post post, @Param("reactionType") PostReactionType reactionType);
    
    // Get reaction statistics for a post
    @Query("SELECT pr.reactionType, COUNT(pr) FROM PostReaction pr WHERE pr.post = :post GROUP BY pr.reactionType")
    List<Object[]> getReactionStatsByPost(@Param("post") Post post);
    
    // Get users who reacted with specific reaction
    @Query("SELECT pr FROM PostReaction pr " +
           "LEFT JOIN FETCH pr.user u " +
           "WHERE pr.post = :post AND pr.reactionType = :reactionType " +
           "ORDER BY pr.createdAt DESC")
    List<PostReaction> findByPostAndReactionTypeWithUser(@Param("post") Post post, @Param("reactionType") PostReactionType reactionType);
    
    // Get all reactions for a post with user details
    @Query("SELECT pr FROM PostReaction pr " +
           "LEFT JOIN FETCH pr.user u " +
           "WHERE pr.post = :post " +
           "ORDER BY pr.createdAt DESC")
    List<PostReaction> findByPostWithUser(@Param("post") Post post);
    
    // Top reacted posts
    @Query("SELECT pr.post, COUNT(pr) as reactionCount FROM PostReaction pr " +
    "WHERE pr.createdAt >= :startDate " +
           "GROUP BY pr.post " +
           "ORDER BY reactionCount DESC")
    List<Object[]> findTopReactedPostsThisWeek(@Param("startDate") java.time.LocalDateTime startDate);
    
    // User's reaction history
    @Query("SELECT pr FROM PostReaction pr " +
           "LEFT JOIN FETCH pr.post p " +
           "WHERE pr.user = :user " +
           "ORDER BY pr.createdAt DESC")
    Page<PostReaction> findByUserWithPost(@Param("user") User user, Pageable pageable);
    
    // Posts user has reacted to with specific reaction
    @Query("SELECT pr.post FROM PostReaction pr WHERE pr.user = :user AND pr.reactionType = :reactionType")
    List<Post> findPostsUserReactedWith(@Param("user") User user, @Param("reactionType") PostReactionType reactionType);
    
    // Check if user has reacted to post
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END FROM PostReaction pr WHERE pr.post = :post AND pr.user = :user")
    boolean existsByPostAndUser(@Param("post") Post post, @Param("user") User user);
    
    // Delete user's reaction to post
    void deleteByPostAndUser(Post post, User user);
    
    // Bulk operations
    @Query("SELECT pr FROM PostReaction pr WHERE pr.post.id IN :postIds")
    List<PostReaction> findByPostIds(@Param("postIds") List<Long> postIds);
    
    // Popular reaction types globally
    @Query("SELECT pr.reactionType, COUNT(pr) FROM PostReaction pr GROUP BY pr.reactionType ORDER BY COUNT(pr) DESC")
    List<Object[]> getGlobalReactionStats();
}

