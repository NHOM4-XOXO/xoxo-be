package com.nhom4.xoxo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom4.xoxo.dto.res.PostReactionResponse;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.PostReaction;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostReactionType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.PostReactionRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.PostReactionNotificationService;
import com.nhom4.xoxo.service.PostReactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(transactionManager = "transactionManager")
public class PostReactionServiceImpl implements PostReactionService {

    private final PostReactionRepository postReactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PostReactionNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public PostReactionResponse addReaction(Long postId, Long userId, PostReactionType reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Check if user already reacted
        Optional<PostReaction> existingReaction = postReactionRepository.findByPostAndUser(post, user);
        
        PostReaction reaction;
        boolean isNewReaction = false;
        
        if (existingReaction.isPresent()) {
            // Update existing reaction
            reaction = existingReaction.get();
            PostReactionType oldType = reaction.getReactionType();
            reaction.setReactionType(reactionType);
            reaction = postReactionRepository.save(reaction);
            
            // Update reaction summary
            updateReactionSummary(post, oldType, reactionType);
        } else {
            // Create new reaction
            reaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            reaction = postReactionRepository.save(reaction);
            isNewReaction = true;
            
            // Update counts
            post.setReactionCount(post.getReactionCount() + 1);
            updateReactionSummary(post, null, reactionType);
        }

        // Save post with updated counts
        postRepository.save(post);

        // Send real-time notification
        notificationService.sendReactionAddedNotification(post, user, reactionType);

        return convertToResponse(reaction);
    }

    @Override
    public void removeReaction(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Optional<PostReaction> existingReaction = postReactionRepository.findByPostAndUser(post, user);
        
        if (existingReaction.isPresent()) {
            PostReaction reaction = existingReaction.get();
            PostReactionType removedType = reaction.getReactionType();
            
            postReactionRepository.delete(reaction);
            
            // Update counts
            post.setReactionCount(Math.max(0, post.getReactionCount() - 1));
            updateReactionSummary(post, removedType, null);
            postRepository.save(post);

            // Send real-time notification
            notificationService.sendReactionRemovedNotification(post, user, removedType);
        }
    }

    @Override
    public PostReactionResponse updateReaction(Long postId, Long userId, PostReactionType newReactionType) {
        return addReaction(postId, userId, newReactionType); // addReaction handles updates
    }

    @Override
    public PostReactionResponse getUserReaction(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Optional<PostReaction> reaction = postReactionRepository.findByPostAndUser(post, user);
        return reaction.map(this::convertToResponse).orElse(null);
    }

    @Override
    public List<PostReactionResponse> getPostReactions(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        List<PostReaction> reactions = postReactionRepository.findByPostWithUser(post);
        return reactions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PostReactionResponse> getPostReactionsPaginated(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        Page<PostReaction> reactions = postReactionRepository.findByPostOrderByCreatedAtDesc(post, pageable);
        return reactions.map(this::convertToResponse);
    }

    @Override
    public List<PostReactionResponse> getPostReactionsByType(Long postId, PostReactionType reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        List<PostReaction> reactions = postReactionRepository.findByPostAndReactionTypeWithUser(post, reactionType);
        return reactions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PostReactionResponse> getPostReactionsByTypePaginated(Long postId, PostReactionType reactionType, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        // For paginated version, we'll use the basic query and filter
        Page<PostReaction> allReactions = postReactionRepository.findByPostOrderByCreatedAtDesc(post, pageable);
        return allReactions.map(this::convertToResponse);
    }

    @Override
    public Map<PostReactionType, Long> getReactionStats(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        List<Object[]> stats = postReactionRepository.getReactionStatsByPost(post);
        Map<PostReactionType, Long> reactionStats = new HashMap<>();
        
        for (Object[] stat : stats) {
            PostReactionType type = (PostReactionType) stat[0];
            Long count = ((Number) stat[1]).longValue();
            reactionStats.put(type, count);
        }
        
        return reactionStats;
    }

    @Override
    public long getTotalReactionCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        return postReactionRepository.countByPost(post);
    }

    @Override
    public long getReactionCountByType(Long postId, PostReactionType reactionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        return postReactionRepository.countByPostAndReactionType(post, reactionType);
    }

    @Override
    public Page<PostReactionResponse> getUserReactionHistory(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        Page<PostReaction> reactions = postReactionRepository.findByUserWithPost(user, pageable);
        return reactions.map(this::convertToResponse);
    }

    @Override
    public List<Long> getPostsUserReactedWith(Long userId, PostReactionType reactionType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        List<Post> posts = postReactionRepository.findPostsUserReactedWith(user, reactionType);
        return posts.stream().map(Post::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getMostReactedPosts(int limit) {
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        List<Object[]> topPosts = postReactionRepository.findTopReactedPostsThisWeek(sevenDaysAgo);
        return topPosts.stream()
                .limit(limit)
                .map(row -> ((Post) row[0]).getId())
                .collect(Collectors.toList());
    }

    @Override
    public Map<PostReactionType, Long> getGlobalReactionStats() {
        List<Object[]> stats = postReactionRepository.getGlobalReactionStats();
        Map<PostReactionType, Long> globalStats = new HashMap<>();
        
        for (Object[] stat : stats) {
            PostReactionType type = (PostReactionType) stat[0];
            Long count = ((Number) stat[1]).longValue();
            globalStats.put(type, count);
        }
        
        return globalStats;
    }

    @Override
    public void removeAllReactions(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        
        List<PostReaction> reactions = postReactionRepository.findByPost(post);
        postReactionRepository.deleteAll(reactions);
        
        // Reset counts
        post.setReactionCount(0);
        post.setLikeCount(0);
        post.setReactionSummary("{}");
        postRepository.save(post);
    }

    @Override
    public void removeUserReactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // This would require a custom query to delete all user's reactions
        // For now, we'll implement it later when needed
    }

    @Override
    public boolean hasUserReacted(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        return postReactionRepository.existsByPostAndUser(post, user);
    }

    @Override
    public PostReactionType getUserReactionType(Long postId, Long userId) {
        PostReactionResponse reaction = getUserReaction(postId, userId);
        return reaction != null ? reaction.getReactionType() : null;
    }

    // Helper methods
    private void updateReactionSummary(Post post, PostReactionType oldType, PostReactionType newType) {
        try {
            Map<String, Integer> summary = parseReactionSummary(post.getReactionSummary());
            
            // Remove old reaction
            if (oldType != null) {
                summary.put(oldType.name(), Math.max(0, summary.getOrDefault(oldType.name(), 0) - 1));
                if (summary.get(oldType.name()) == 0) {
                    summary.remove(oldType.name());
                }
            }
            
            // Add new reaction
            if (newType != null) {
                summary.put(newType.name(), summary.getOrDefault(newType.name(), 0) + 1);
            }
            
            post.setReactionSummary(objectMapper.writeValueAsString(summary));
            
            // Update like count for backward compatibility
            post.setLikeCount(summary.getOrDefault("LIKE", 0));
            
        } catch (Exception e) {
            log.error("Error updating reaction summary for post {}: {}", post.getId(), e.getMessage());
        }
    }

    private Map<String, Integer> parseReactionSummary(String reactionSummary) {
        if (reactionSummary == null || reactionSummary.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Integer> summary = objectMapper.readValue(reactionSummary, Map.class);
            return summary;
        } catch (JsonProcessingException e) {
            log.error("Error parsing reaction summary: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private void sendReactionNotification(Post post, User user, PostReactionType reactionType, String action) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "POST_REACTION_" + action,
                "postId", post.getId(),
                "userId", user.getId(),
                "userName", user.getFirstName() + " " + user.getLastName(),
                "userAvatar", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "reactionType", reactionType.name(),
                "emoji", reactionType.getEmoji(),
                "timestamp", System.currentTimeMillis()
            );

            // Send to post author (if not self-reaction)
            if (!post.getAuthor().getId().equals(user.getId())) {
                messagingTemplate.convertAndSendToUser(
                    post.getAuthor().getId().toString(),
                    "/queue/post-reactions",
                    notification
                );
            }

            // Send to all followers/friends interested in this post
            messagingTemplate.convertAndSend("/topic/post/" + post.getId() + "/reactions", notification);
            
        } catch (Exception e) {
            log.error("Error sending reaction notification: {}", e.getMessage());
        }
    }

    private PostReactionResponse convertToResponse(PostReaction reaction) {
        return PostReactionResponse.builder()
                .id(reaction.getId())
                .postId(reaction.getPost().getId())
                .userId(reaction.getUser().getId())
                .userName(reaction.getUser().getFirstName() + " " + reaction.getUser().getLastName())
                .userAvatar(reaction.getUser().getAvatarUrl())
                .reactionType(reaction.getReactionType())
                .emoji(reaction.getReactionType().getEmoji())
                .displayName(reaction.getReactionType().getDisplayName())
                .createdAt(reaction.getCreatedAt())
                .build();
    }
}
