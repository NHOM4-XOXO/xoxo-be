package com.nhom4.xoxo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.PostRequest;
import com.nhom4.xoxo.dto.res.CommentItemResponse;
import com.nhom4.xoxo.dto.res.FacebookStylePostResponse;
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.dto.res.PostReactionResponse;
import com.nhom4.xoxo.dto.res.PostResponse;
import com.nhom4.xoxo.dto.res.PostWithMediaResponse;
import com.nhom4.xoxo.dto.res.SharePostItemResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostReactionType;
import com.nhom4.xoxo.service.PostReactionService;
import com.nhom4.xoxo.service.PostService;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.UserService;
import com.nhom4.xoxo.service.NewsFeedIntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/posts")
@Slf4j
public class PostController {

    private final PostService postService;
    private final PostReactionService postReactionService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final CloudinaryService cloudinaryService;
    private final NewsFeedIntegrationService newsFeedIntegrationService;

    public PostController(PostService postService, PostReactionService postReactionService, UserService userService,
            ModelMapper modelMapper, CloudinaryService cloudinaryService, NewsFeedIntegrationService newsFeedIntegrationService) {
        this.postService = postService;
        this.postReactionService = postReactionService;
        this.userService = userService;
        this.modelMapper = modelMapper; // kept for future mappings
        this.cloudinaryService = cloudinaryService;
        this.newsFeedIntegrationService = newsFeedIntegrationService;
    }

    @Operation(summary = "Tạo bài viết mới", description = "Yêu cầu đã đăng nhập. Tạo bài viết mới.", responses = {
            @ApiResponse(responseCode = "200", description = "Tạo bài viết thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping
    public ResponseEntity<WrapRes<?>> createPost(@RequestBody @Valid PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User author = userService.findByEmail(email);

        // Map PostRequest to Post entity
        Post post = Post.builder()
                .content(postRequest.getContent())
                .status(postRequest.getStatus())
                .type(postRequest.getType())
                .location(postRequest.getLocation())
                .hashtags(postRequest.getHashtags())
                .isPublic(postRequest.getIsPublic())
                .allowComments(postRequest.getAllowComments())
                .allowLikes(postRequest.getAllowLikes())
                .allowShares(postRequest.getAllowShares())
                .author(author)
                .build();

        // Set parent post if provided
        if (postRequest.getParentPostId() != null) {
            Post parentPost = postService.getPostById(postRequest.getParentPostId()).get();
            post.setParentPost(parentPost);
        }

        Post createdPost = postService.createPost(post);

        // Add media if provided
        if (postRequest.getMediaIds() != null && !postRequest.getMediaIds().isEmpty()) {
            for (Long mediaId : postRequest.getMediaIds()) {
                postService.addMediaToPost(createdPost.getId(), mediaId);
            }
        }

        // Update NewsFeed for friends
        newsFeedIntegrationService.onPostCreated(createdPost);

        // Build response with media
        PostItemResponse postItem = postService.getPostItemById(createdPost.getId()).orElse(null);
        List<Media> media = postService.getPostMedia(createdPost.getId());
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();
        PostWithMediaResponse res = PostWithMediaResponse.builder()
                .post(postItem)
                .media(mediaResponses)
                .build();
        return ResponseEntity.ok(WrapRes.success(res));
    }

    @Operation(summary = "Lấy bài viết theo ID", description = "Lấy thông tin chi tiết bài viết theo ID", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bài viết thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<WrapRes<?>> getPostById(@PathVariable Long postId) {
        PostItemResponse post = postService.getPostItemById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.ok(WrapRes.error("Post not found"));
        }
        List<Media> media = postService.getPostMedia(postId);
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();
        PostWithMediaResponse res = PostWithMediaResponse.builder()
                .post(post)
                .media(mediaResponses)
                .build();
        return ResponseEntity.ok(WrapRes.success(res));
    }

    @Operation(summary = "Lấy tất cả bài viết public", description = "Lấy danh sách tất cả bài viết public", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công")
    })
    @GetMapping("/public")
    public ResponseEntity<WrapRes<?>> getPublicPosts() {
        List<PostWithMediaResponse> posts = postService.getPublicPostsWithMedia();

        return ResponseEntity.ok(WrapRes.success(posts));
    }

    @Operation(summary = "Lấy bài viết theo tác giả", description = "Lấy danh sách bài viết của một tác giả", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công")
    })
    @GetMapping("/author/{userId}")
    public ResponseEntity<WrapRes<?>> getPostsByAuthor(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);
        User author = userService.findById(userId);
        List<PostWithMediaResponse> posts = postService.getPostsByAuthor(author);

        return ResponseEntity.ok(WrapRes.success(posts));
    }

    @Operation(summary = "Lấy tất cả bài viết của tôi", description = "Trả về tất cả bài viết do user đang đăng nhập tạo (kèm media)", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công")
    })
    @GetMapping("/me")
    public ResponseEntity<WrapRes<?>> getMyPosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User me = userService.findByEmail(email);
        List<PostWithMediaResponse> myPostItems = postService.getPostsByCurrentUser(me);

        return ResponseEntity.ok(WrapRes.success(myPostItems));
    }

    @Operation(summary = "Lấy media của bài viết", description = "Lấy danh sách media của một bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy media thành công")
    })
    @GetMapping("/{postId}/media")
    public ResponseEntity<WrapRes<?>> getPostMedia(@PathVariable Long postId) {
        List<Media> media = postService.getPostMedia(postId);
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();
        return ResponseEntity.ok(WrapRes.success(mediaResponses));
    }

    @Operation(summary = "Lấy bài viết cùng media", description = "Trả về PostItemResponse và danh sách MediaResponse cho bài viết")
    @GetMapping("/{postId}/details")
    public ResponseEntity<WrapRes<?>> getPostWithMedia(@PathVariable Long postId) {
        PostItemResponse post = postService.getPostItemById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.ok(WrapRes.error("Post not found"));
        }
        List<Media> media = postService.getPostMedia(postId);
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();
        PostWithMediaResponse res = PostWithMediaResponse.builder()
                .post(post)
                .media(mediaResponses)
                .build();
        return ResponseEntity.ok(WrapRes.success(res));
    }

    @Operation(summary = "Lấy comments của bài viết", description = "Lấy danh sách comments của một bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy comments thành công")
    })
    @GetMapping("/{postId}/comments")
    public ResponseEntity<WrapRes<?>> getPostComments(@PathVariable Long postId) {
        return ResponseEntity.ok(WrapRes.success(postService.getCommentsOfPost(postId)));
    }

    @Operation(summary = "Lấy shares của bài viết", description = "Lấy danh sách shares của một bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy shares thành công")
    })
    @GetMapping("/{postId}/shares")
    public ResponseEntity<WrapRes<?>> getPostShares(@PathVariable Long postId) {
        return ResponseEntity.ok(WrapRes.success(postService.getSharesOfPost(postId)));
    }

    @Operation(summary = "Danh sách users đã like bài viết", description = "Trả về user cơ bản của những người đã like")
    @GetMapping("/{postId}/likes")
    public ResponseEntity<WrapRes<?>> getPostLikes(@PathVariable Long postId) {
        return ResponseEntity.ok(WrapRes.success(postService.getUsersLikedPost(postId)));
    }

    @Operation(summary = "Thêm media cho bài viết", description = "Thêm một hoặc nhiều media vào bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Thêm media thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy post hoặc media")
    })
    @PostMapping("/{postId}/media")
    public ResponseEntity<WrapRes<?>> addMediaToPost(
            @PathVariable Long postId,
            @RequestParam("mediaIds") List<Long> mediaIds) {
        for (Long mediaId : mediaIds) {
            postService.addMediaToPost(postId, mediaId);
        }
        PostItemResponse post = postService.getPostItemById(postId).orElse(null);
        List<Media> media = postService.getPostMedia(postId);
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();
        PostWithMediaResponse res = PostWithMediaResponse.builder()
                .post(post)
                .media(mediaResponses)
                .build();
        return ResponseEntity.ok(WrapRes.success(res));
    }

    @Operation(summary = "Xóa media khỏi bài viết", description = "Xóa media khỏi bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa media thành công")
    })
    @DeleteMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<WrapRes<?>> removeMediaFromPost(@PathVariable Long postId, @PathVariable Long mediaId) {
        postService.removeMediaFromPost(postId, mediaId);
        return ResponseEntity.ok(WrapRes.success("Media removed from post successfully"));
    }

    @Operation(summary = "Cập nhật bài viết", description = "Cập nhật thông tin bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật bài viết thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết")
    })
    @PutMapping("/{postId}")
    public ResponseEntity<WrapRes<?>> updatePost(@PathVariable Long postId,
            @RequestBody @Valid PostRequest postRequest) {
        // Get existing post
        Post existingPost = postService.getPostById(postId).get();

        // Update fields from request
        existingPost.setContent(postRequest.getContent());
        existingPost.setStatus(postRequest.getStatus());
        existingPost.setType(postRequest.getType());
        existingPost.setLocation(postRequest.getLocation());
        existingPost.setHashtags(postRequest.getHashtags());
        existingPost.setPublic(postRequest.getIsPublic());
        existingPost.setAllowComments(postRequest.getAllowComments());
        existingPost.setAllowLikes(postRequest.getAllowLikes());
        existingPost.setAllowShares(postRequest.getAllowShares());
        List<Long> mediaIds = postRequest.getMediaIds();
        if (mediaIds != null && !mediaIds.isEmpty()) {
            for (Long mediaId : mediaIds) {
                postService.addMediaToPost(postId, mediaId);
            }
        }
        // Update parent post if provided
        if (postRequest.getParentPostId() != null) {
            Post parentPost = postService.getPostById(postRequest.getParentPostId()).get();
            existingPost.setParentPost(parentPost);
        }
      
        Post updatedPost = postService.updatePost(postId, existingPost);
        PostResponse postResponse = mapToPostResponse(updatedPost);
        return ResponseEntity.ok(WrapRes.success(postResponse));
    }


    @Operation(summary = "like post", description = " like  post", responses = {
            @ApiResponse(responseCode = "200", description = "like  post")
    })
    @PostMapping("/{postId}/like")
    public ResponseEntity<WrapRes<?>> toggleLike(@PathVariable Long postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        boolean liked = postService.toggleLike(postId, user);
        
        // Update NewsFeed if liked
        if (liked) {
            Post post = postService.getPostById(postId).orElse(null);
            if (post != null) {
                newsFeedIntegrationService.onPostLiked(post, user);
            }
        }
        
        return ResponseEntity.ok(WrapRes.success(liked ? "LIKED" : "UNLIKED"));
    }

    @Operation(summary = " comment post ", description = "Comment post", responses = {
            @ApiResponse(responseCode = "200", description = "Comment post")
    })
    @PostMapping("/{postId}/comment")
    public ResponseEntity<WrapRes<?>> addComment(@PathVariable Long postId, @RequestParam("content") String content,
            @RequestParam(value = "parentCommentId", required = false) Long parentCommentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        CommentItemResponse c = postService.addComment(postId, user, content, parentCommentId);
        
        // Update NewsFeed for comment
        Post post = postService.getPostById(postId).orElse(null);
        if (post != null) {
            newsFeedIntegrationService.onPostCommented(post, user);
        }
        
        return ResponseEntity.ok(WrapRes.success(c));
    }

    @Operation(summary = "Share post", description = "Share a post to your friends' feeds")
    @PostMapping("/{postId}/share")
    public ResponseEntity<WrapRes<?>> sharePost(@PathVariable Long postId, @RequestParam("shareContent") String shareContent) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        
        Post post = postService.getPostById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest()
                .body(WrapRes.error("Post not found"));
        }
        
        SharePostItemResponse shareResponse = postService.sharePost(postId, user, shareContent);
        
        // Update NewsFeed for share
        newsFeedIntegrationService.handlePostShare(post, user, shareContent);
        
        return ResponseEntity.ok(WrapRes.success(shareResponse));
    }

    @Operation(summary = "Delete post", description = "Delete a post and clean up NewsFeed")
    @DeleteMapping("/{postId}")
    public ResponseEntity<WrapRes<?>> deletePost(@PathVariable Long postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        
        Post post = postService.getPostById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest()
                .body(WrapRes.error("Post not found"));
        }
        
        // Check if user owns the post
        if (!post.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403)
                .body(WrapRes.error("You can only delete your own posts"));
        }
        
        // Delete post
        postService.deletePost(postId);
        
        // Clean up NewsFeed
        newsFeedIntegrationService.onPostDeleted(postId);
        
        return ResponseEntity.ok(WrapRes.success("Post deleted successfully"));
    }

    @Operation(summary = "Lấy subtree của 1 comment", description = "Trả về node và tất cả descendants của nó")
    @GetMapping("/comments/{commentId}/subtree")
    public ResponseEntity<WrapRes<?>> getCommentSubtree(@PathVariable Long commentId) {
        return ResponseEntity.ok(WrapRes.success(postService.getCommentSubtree(commentId)));
    }

    @Operation(summary = "Đếm tổng số replies (mọi cấp)", description = "Đếm tất cả descendants của comment")
    @GetMapping("/comments/{commentId}/replies/count-all")
    public ResponseEntity<WrapRes<?>> countAllReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(WrapRes.success(postService.countAllRepliesForComment(commentId)));
    }


    @Operation(summary = "Tăng view count", description = "Tăng số lượt xem của bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Tăng view count thành công")
    })

    // Helper method to map Post entity to PostResponse
    private PostResponse mapToPostResponse(Post post) {
        // Map author to UserResponse manually to avoid circular reference
        UserResponse authorResponse = null;
        if (post.getAuthor() != null) {
            try {
                User author = post.getAuthor();
                authorResponse = UserResponse.builder()
                        .id(author.getId())
                        .email(author.getEmail())
                        .firstName(author.getFirstName())
                        .lastName(author.getLastName())
                        .roles(author.getRoles())
                        .dateOfBirth(author.getDateOfBirth())
                        .gender(author.getGender())
                        .avatarUrl(author.getAvatarUrl())
                        .coverUrl(author.getCoverUrl())
                        .bio(author.getBio())
                        .createdAt(author.getCreatedAt())
                        .updatedAt(author.getUpdatedAt())
                        .enabled(author.isEnabled())
                        .username(author.getUsername())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to map author for post {}: {}", post.getId(), e.getMessage());
            }
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .status(post.getStatus())
                .type(post.getType())
                .location(post.getLocation())
                .hashtags(post.getHashtags())
                .isPublic(post.isPublic())
                .allowComments(post.isAllowComments())
                .allowLikes(post.isAllowLikes())
                .allowShares(post.isAllowShares())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .viewCount(post.getViewCount())
                .author(authorResponse)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // Helper method to map Media entity to MediaResponse
    private MediaResponse mapToMediaResponse(Media media) {
        UserResponse uploadedByResponse = null;
        if (media.getUploadedBy() != null) {
            try {
                User uploadedBy = media.getUploadedBy();
                uploadedByResponse = UserResponse.builder()
                        .id(uploadedBy.getId())
                        .email(uploadedBy.getEmail())
                        .firstName(uploadedBy.getFirstName())
                        .lastName(uploadedBy.getLastName())
                        .roles(uploadedBy.getRoles())
                        .dateOfBirth(uploadedBy.getDateOfBirth())
                        .gender(uploadedBy.getGender())
                        .avatarUrl(uploadedBy.getAvatarUrl())
                        .coverUrl(uploadedBy.getCoverUrl())
                        .bio(uploadedBy.getBio())
                        .createdAt(uploadedBy.getCreatedAt())
                        .updatedAt(uploadedBy.getUpdatedAt())
                        .enabled(uploadedBy.isEnabled())
                        .username(uploadedBy.getUsername())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to map uploadedBy for media {}: {}", media.getId(), e.getMessage());
            }
        }

        return MediaResponse.builder()
                .id(media.getId())
                .mediaUrl(cloudinaryService.buildCloudinaryUrl(media.getMediaUrl(), media.getMediaType()))
                .mediaType(media.getMediaType())
                .originalFilename(media.getOriginalFilename())
                .fileSize(media.getFileSize())
                .uploadedBy(uploadedByResponse)
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }

    // ==================== Facebook-style Reaction Endpoints ====================

    @Operation(summary = "React to post (Facebook style)", description = "Add Facebook-style reaction to post")
    @PostMapping("/{postId}/react/{reactionType}")
    public ResponseEntity<WrapRes<Map<String, Object>>> reactToPost(
            @PathVariable Long postId,
            @PathVariable PostReactionType reactionType) {

        var reaction = postReactionService.addReaction(postId, getCurrentUser().getId(), reactionType);
        var stats = postReactionService.getReactionStats(postId);

        return ResponseEntity.ok(WrapRes.success(Map.of(
                "reaction", reaction,
                "stats", stats,
                "message", "Reacted with " + reactionType.getDisplayName())));
    }

    @Operation(summary = "Remove reaction from post", description = "Remove user's reaction from post")
    @DeleteMapping("/{postId}/react")
    public ResponseEntity<WrapRes<Map<String, Object>>> removeReaction(@PathVariable Long postId) {
        postReactionService.removeReaction(postId, getCurrentUser().getId());
        var stats = postReactionService.getReactionStats(postId);

        return ResponseEntity.ok(WrapRes.success(Map.of(
                "stats", stats,
                "message", "Reaction removed")));
    }

    @Operation(summary = "Get post reactions", description = "Get all reactions for a post with pagination")
    @GetMapping("/{postId}/reactions")
    public ResponseEntity<WrapRes<org.springframework.data.domain.Page<PostReactionResponse>>> getPostReactions(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var reactions = postReactionService.getPostReactionsPaginated(postId, pageable);
        return ResponseEntity.ok(WrapRes.success(reactions));
    }

    @Operation(summary = "Get reaction statistics", description = "Get Facebook-style reaction statistics for post")
    @GetMapping("/{postId}/reaction-stats")
    public ResponseEntity<WrapRes<Map<String, Object>>> getPostReactionStats(@PathVariable Long postId) {
        var stats = postReactionService.getReactionStats(postId);
        long totalReactions = postReactionService.getTotalReactionCount(postId);

        // Create Facebook-style summary
        StringBuilder summary = new StringBuilder();
        stats.entrySet().stream()
                .sorted(Map.Entry.<PostReactionType, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    if (summary.length() > 0)
                        summary.append(", ");
                    summary.append(entry.getKey().getEmoji()).append(" ").append(entry.getValue());
                });

        return ResponseEntity.ok(WrapRes.success(Map.of(
                "totalReactions", totalReactions,
                "reactionBreakdown", stats,
                "reactionSummary", summary.toString(),
                "topReactions", stats.entrySet().stream()
                        .sorted(Map.Entry.<PostReactionType, Long>comparingByValue().reversed())
                        .limit(3)
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                java.util.LinkedHashMap::new)))));
    }

    @Operation(summary = "Check user reaction", description = "Check if current user has reacted to post")
    @GetMapping("/{postId}/my-reaction")
    public ResponseEntity<WrapRes<Map<String, Object>>> checkMyReaction(@PathVariable Long postId) {
        Long userId = getCurrentUser().getId();
        boolean hasReacted = postReactionService.hasUserReacted(postId, userId);
        var reactionType = postReactionService.getUserReactionType(postId, userId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("hasReacted", hasReacted);
        responseMap.put("reactionType", reactionType != null ? reactionType.name() : null);
        responseMap.put("emoji", reactionType != null ? reactionType.getEmoji() : null);
        responseMap.put("displayName", reactionType != null ? reactionType.getDisplayName() : null);

        return ResponseEntity.ok(WrapRes.success(responseMap));
    }

    @Operation(summary = "Get Facebook-style post data", description = "Get post with complete Facebook-style reaction and engagement data")
    @GetMapping("/{postId}/facebook-style")
    public ResponseEntity<WrapRes<FacebookStylePostResponse>> getFacebookStylePost(
            @PathVariable Long postId) {
        // Get basic post data
        PostItemResponse post = postService.getPostItemById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.ok(WrapRes.error("Post not found"));
        }

        // Get media
        List<Media> media = postService.getPostMedia(postId);
        List<MediaResponse> mediaResponses = media.stream()
                .map(this::mapToMediaResponse)
                .toList();

        // Get reaction stats
        var reactionStats = postReactionService.getReactionStats(postId);
        long totalReactions = postReactionService.getTotalReactionCount(postId);

        // Get user's interaction
        Long userId = getCurrentUser().getId();
        var userReaction = postReactionService.getUserReactionType(postId, userId);

        // Create Facebook-style summary
        StringBuilder reactionSummary = new StringBuilder();
        var topReactions = reactionStats.entrySet().stream()
                .sorted(Map.Entry.<PostReactionType, Long>comparingByValue().reversed())
                .limit(3)
                .collect(java.util.stream.Collectors.toList());

        topReactions.forEach(entry -> {
            if (reactionSummary.length() > 0)
                reactionSummary.append(", ");
            reactionSummary.append(entry.getKey().getEmoji()).append(" ").append(entry.getValue());
        });

        // Build response
        var response = FacebookStylePostResponse.builder()
                .id(post.id())
                .content(post.content())
                .author(UserResponse.builder()
                        .id(post.authorId())
                        .firstName(post.authorFirstName())
                        .lastName(post.authorLastName())
                        .avatarUrl(post.authorAvatarUrl())
                        .build())
                .createdAt(post.createdAt())
                .updatedAt(post.updatedAt())
                .media(mediaResponses)
                .engagement(FacebookStylePostResponse.EngagementData.builder()
                        .totalReactions((int) totalReactions)
                        .reactionBreakdown(reactionStats)
                        .reactionSummary(reactionSummary.toString())
                        .commentCount(post.commentCount())
                        .shareCount(post.shareCount())
                        .viewCount(post.viewCount())
                        .build())
                .userInteraction(FacebookStylePostResponse.UserInteraction.builder()
                        .userReaction(userReaction)
                        .hasLiked(userReaction == PostReactionType.LIKE)
                        .hasCommented(checkUserHasCommented(postId, userId))
                        .hasShared(checkUserHasShared(postId, userId))
                        .hasViewed(true) // Assume viewed since they're requesting it
                        .build())
                .build();

        return ResponseEntity.ok(WrapRes.success(response));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findByEmail(email);
    }

    private boolean checkUserHasCommented(Long postId, Long userId) {
        try {
            // Check if user has any comments on this post
            var comments = postService.getCommentsOfPost(postId);
            return comments.stream().anyMatch(comment -> comment.authorId().equals(userId));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkUserHasShared(Long postId, Long userId) {
        try {
            // Check if user has shared this post
            var shares = postService.getSharesOfPost(postId);
            return shares.stream().anyMatch(share -> share.sharerId().equals(userId));
        } catch (Exception e) {
            return false;
        }
    }
}