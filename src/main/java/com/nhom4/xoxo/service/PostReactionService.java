package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.res.PostReactionResponse;
import com.nhom4.xoxo.enums.PostReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PostReactionService {
    
    // Basic reaction operations
    PostReactionResponse addReaction(Long postId, Long userId, PostReactionType reactionType);
    void removeReaction(Long postId, Long userId);
    PostReactionResponse updateReaction(Long postId, Long userId, PostReactionType newReactionType);
    
    // Get reactions
    PostReactionResponse getUserReaction(Long postId, Long userId);
    List<PostReactionResponse> getPostReactions(Long postId);
    Page<PostReactionResponse> getPostReactionsPaginated(Long postId, Pageable pageable);
    
    // Get reactions by type
    List<PostReactionResponse> getPostReactionsByType(Long postId, PostReactionType reactionType);
    Page<PostReactionResponse> getPostReactionsByTypePaginated(Long postId, PostReactionType reactionType, Pageable pageable);
    
    // Reaction statistics
    Map<PostReactionType, Long> getReactionStats(Long postId);
    long getTotalReactionCount(Long postId);
    long getReactionCountByType(Long postId, PostReactionType reactionType);
    
    // User reaction history
    Page<PostReactionResponse> getUserReactionHistory(Long userId, Pageable pageable);
    List<Long> getPostsUserReactedWith(Long userId, PostReactionType reactionType);
    
    // Popular content
    List<Long> getMostReactedPosts(int limit);
    Map<PostReactionType, Long> getGlobalReactionStats();
    
    // Bulk operations
    void removeAllReactions(Long postId);
    void removeUserReactions(Long userId);
    
    // Check methods
    boolean hasUserReacted(Long postId, Long userId);
    PostReactionType getUserReactionType(Long postId, Long userId);
}












