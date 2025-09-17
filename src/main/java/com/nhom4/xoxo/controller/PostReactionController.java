package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.PostReactionResponse;
import com.nhom4.xoxo.dto.res.PostReactionStatsResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostReactionType;
import com.nhom4.xoxo.service.PostReactionService;
import com.nhom4.xoxo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post Reactions", description = "Facebook-like post reaction system")
public class PostReactionController {

    private final PostReactionService postReactionService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findByEmail(email);
    }

    // ==================== Basic Reaction Operations ====================

    @Operation(summary = "Add reaction to post", description = "Add or update reaction to a post (Facebook-style)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reaction added successfully"),
        @ApiResponse(responseCode = "404", description = "Post not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/{postId}/reactions/{reactionType}")
    public ResponseEntity<WrapRes<PostReactionResponse>> addReaction(
            @Parameter(description = "ID of the post") @PathVariable Long postId,
            @Parameter(description = "Type of reaction") @PathVariable PostReactionType reactionType) {
        
        PostReactionResponse response = postReactionService.addReaction(postId, getCurrentUser().getId(), reactionType);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(summary = "Remove reaction from post", description = "Remove user's reaction from a post")
    @DeleteMapping("/{postId}/reactions")
    public ResponseEntity<WrapRes<Void>> removeReaction(
            @Parameter(description = "ID of the post") @PathVariable Long postId) {
        
        postReactionService.removeReaction(postId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Get user's reaction", description = "Get current user's reaction to a post")
    @GetMapping("/{postId}/reactions/me")
    public ResponseEntity<WrapRes<PostReactionResponse>> getMyReaction(
            @Parameter(description = "ID of the post") @PathVariable Long postId) {
        
        PostReactionResponse response = postReactionService.getUserReaction(postId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(response));
    }

    // ==================== Get Reactions ====================

    @Operation(summary = "Get all reactions for post", description = "Get all reactions for a specific post")
    @GetMapping("/{postId}/reactions")
    public ResponseEntity<WrapRes<Page<PostReactionResponse>>> getPostReactions(
            @Parameter(description = "ID of the post") @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PostReactionResponse> reactions = postReactionService.getPostReactionsPaginated(postId, pageable);
        return ResponseEntity.ok(WrapRes.success(reactions));
    }

    @Operation(summary = "Get reactions by type", description = "Get all reactions of specific type for a post")
    @GetMapping("/{postId}/reactions/type/{reactionType}")
    public ResponseEntity<WrapRes<List<PostReactionResponse>>> getPostReactionsByType(
            @Parameter(description = "ID of the post") @PathVariable Long postId,
            @Parameter(description = "Type of reaction") @PathVariable PostReactionType reactionType) {
        
        List<PostReactionResponse> reactions = postReactionService.getPostReactionsByType(postId, reactionType);
        return ResponseEntity.ok(WrapRes.success(reactions));
    }

    // ==================== Reaction Statistics ====================

    @Operation(summary = "Get reaction statistics", description = "Get detailed reaction statistics for a post")
    @GetMapping("/{postId}/reactions/stats")
    public ResponseEntity<WrapRes<Map<String, Object>>> getReactionStats(
            @Parameter(description = "ID of the post") @PathVariable Long postId) {
        
        Map<PostReactionType, Long> stats = postReactionService.getReactionStats(postId);
        long totalReactions = postReactionService.getTotalReactionCount(postId);
        
        // Find top reaction
        PostReactionType topReaction = stats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        Map<String, Object> response = Map.of(
            "totalReactions", totalReactions,
            "reactionBreakdown", stats,
            "topReaction", topReaction != null ? Map.of(
                "type", topReaction.name(),
                "emoji", topReaction.getEmoji(),
                "count", stats.get(topReaction)
            ) : null
        );
        
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(summary = "Get reaction count by type", description = "Get count of specific reaction type for a post")
    @GetMapping("/{postId}/reactions/count/{reactionType}")
    public ResponseEntity<WrapRes<Map<String, Long>>> getReactionCountByType(
            @Parameter(description = "ID of the post") @PathVariable Long postId,
            @Parameter(description = "Type of reaction") @PathVariable PostReactionType reactionType) {
        
        long count = postReactionService.getReactionCountByType(postId, reactionType);
        return ResponseEntity.ok(WrapRes.success(Map.of("count", count)));
    }

    // ==================== User Reaction History ====================

    @Operation(summary = "Get user's reaction history", description = "Get paginated history of user's reactions")
    @GetMapping("/reactions/history")
    public ResponseEntity<WrapRes<Page<PostReactionResponse>>> getMyReactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PostReactionResponse> history = postReactionService.getUserReactionHistory(getCurrentUser().getId(), pageable);
        return ResponseEntity.ok(WrapRes.success(history));
    }

    @Operation(summary = "Get posts user reacted with specific reaction", description = "Get posts user reacted to with specific reaction type")
    @GetMapping("/reactions/my-posts/{reactionType}")
    public ResponseEntity<WrapRes<List<Long>>> getMyPostsWithReaction(
            @Parameter(description = "Type of reaction") @PathVariable PostReactionType reactionType) {
        
        List<Long> postIds = postReactionService.getPostsUserReactedWith(getCurrentUser().getId(), reactionType);
        return ResponseEntity.ok(WrapRes.success(postIds));
    }

    // ==================== Popular Content ====================

    @Operation(summary = "Get most reacted posts", description = "Get posts with most reactions this week")
    @GetMapping("/reactions/trending")
    public ResponseEntity<WrapRes<List<Long>>> getMostReactedPosts(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Long> trendingPosts = postReactionService.getMostReactedPosts(limit);
        return ResponseEntity.ok(WrapRes.success(trendingPosts));
    }

    @Operation(summary = "Get global reaction statistics", description = "Get global reaction usage statistics")
    @GetMapping("/reactions/global-stats")
    public ResponseEntity<WrapRes<Map<PostReactionType, Long>>> getGlobalReactionStats() {
        Map<PostReactionType, Long> stats = postReactionService.getGlobalReactionStats();
        return ResponseEntity.ok(WrapRes.success(stats));
    }

    // ==================== Quick Actions (Facebook-style) ====================

    @Operation(summary = "Quick like (Facebook style)", description = "Quick like action - adds LIKE reaction or removes if already liked")
    @PostMapping("/{postId}/like")
    public ResponseEntity<WrapRes<Map<String, Object>>> quickLike(
            @Parameter(description = "ID of the post") @PathVariable Long postId) {
        
        Long userId = getCurrentUser().getId();
        
        // Check current reaction
        PostReactionResponse currentReaction = postReactionService.getUserReaction(postId, userId);
        
        if (currentReaction != null && currentReaction.getReactionType() == PostReactionType.LIKE) {
            // Already liked, so unlike
            postReactionService.removeReaction(postId, userId);
            return ResponseEntity.ok(WrapRes.success(Map.of(
                "action", "UNLIKED",
                "liked", false
            )));
        } else {
            // Add like reaction
            PostReactionResponse response = postReactionService.addReaction(postId, userId, PostReactionType.LIKE);
            return ResponseEntity.ok(WrapRes.success(Map.of(
                "action", "LIKED",
                "liked", true,
                "reaction", response
            )));
        }
    }

    @Operation(summary = "Check if user has reacted", description = "Check if current user has reacted to a post")
    @GetMapping("/{postId}/reactions/check")
    public ResponseEntity<WrapRes<Map<String, Object>>> checkUserReaction(
            @Parameter(description = "ID of the post") @PathVariable Long postId) {
        
        Long userId = getCurrentUser().getId();
        boolean hasReacted = postReactionService.hasUserReacted(postId, userId);
        PostReactionType reactionType = postReactionService.getUserReactionType(postId, userId);
        
        return ResponseEntity.ok(WrapRes.success(Map.of(
            "hasReacted", hasReacted,
            "reactionType", reactionType != null ? reactionType.name() : null,
            "emoji", reactionType != null ? reactionType.getEmoji() : null
        )));
    }
}













