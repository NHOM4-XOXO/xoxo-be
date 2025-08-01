package com.nhom4.xoxo.controller;

import java.util.List;

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
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.dto.res.PostResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.PostService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/posts")
@Slf4j
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public PostController(PostService postService, UserService userService, ModelMapper modelMapper) {
        this.postService = postService;
        this.userService = userService;
        this.modelMapper = modelMapper;
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

       PostResponse postResponse = mapToPostResponse(createdPost);

        
        
        return ResponseEntity.ok(WrapRes.success(postResponse));
    }

    @Operation(summary = "Lấy bài viết theo ID", description = "Lấy thông tin chi tiết bài viết theo ID", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bài viết thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<WrapRes<?>> getPostById(@PathVariable Long postId) {
        Post post = postService.getPostById(postId).get();
        PostResponse postResponse = mapToPostResponse(post);
        return ResponseEntity.ok(WrapRes.success(postResponse));
    }

    @Operation(summary = "Lấy tất cả bài viết public", description = "Lấy danh sách tất cả bài viết public", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công")
    })
    @GetMapping("/public")
    public ResponseEntity<WrapRes<?>> getPublicPosts() {
        List<Post> posts = postService.getPublicPosts();
        List<PostResponse> postResponses = posts.stream()
            .map(this::mapToPostResponse)
            .toList();
        log.info("Found {} public posts", postResponses.size());
        return ResponseEntity.ok(WrapRes.success(postResponses));
    }

    @Operation(summary = "Lấy bài viết theo tác giả", description = "Lấy danh sách bài viết của một tác giả", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công")
    })
    @GetMapping("/author/{userId}")
    public ResponseEntity<WrapRes<?>> getPostsByAuthor(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);
        User author = userService.findById(userId, currentUser);
        List<Post> posts = postService.getPostsByAuthor(author);
        List<PostResponse> postResponses = posts.stream()
            .map(this::mapToPostResponse)
            .toList();
        return ResponseEntity.ok(WrapRes.success(postResponses));
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

    @Operation(summary = "Lấy comments của bài viết", description = "Lấy danh sách comments của một bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy comments thành công")
    })
    @GetMapping("/{postId}/comments")
    public ResponseEntity<WrapRes<?>> getPostComments(@PathVariable Long postId) {
        List<Comment> comments = postService.getPostComments(postId);
        return ResponseEntity.ok(WrapRes.success(comments));
    }

    @Operation(summary = "Lấy shares của bài viết", description = "Lấy danh sách shares của một bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy shares thành công")
    })
    @GetMapping("/{postId}/shares")
    public ResponseEntity<WrapRes<?>> getPostShares(@PathVariable Long postId) {
        List<SharePost> shares = postService.getPostShares(postId);
        return ResponseEntity.ok(WrapRes.success(shares));
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
        return ResponseEntity.ok(WrapRes.success("Media added to post successfully"));
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
    public ResponseEntity<WrapRes<?>> updatePost(@PathVariable Long postId, @RequestBody @Valid PostRequest postRequest) {
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
        
        // Update parent post if provided
        if (postRequest.getParentPostId() != null) {
            Post parentPost = postService.getPostById(postRequest.getParentPostId()).get()  ;
            existingPost.setParentPost(parentPost);
        }
        
        Post updatedPost = postService.updatePost(postId, existingPost);
        PostResponse postResponse = mapToPostResponse(updatedPost);
        return ResponseEntity.ok(WrapRes.success(postResponse));
    }

    @Operation(summary = "Xóa bài viết", description = "Xóa bài viết (soft delete)", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa bài viết thành công")
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<WrapRes<?>> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok(WrapRes.success("Post deleted successfully"));
    }

    @Operation(summary = "Tăng like count", description = "Tăng số lượt like của bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Tăng like count thành công")
    })
    @PostMapping("/{postId}/like")
    public ResponseEntity<WrapRes<?>> incrementLikeCount(@PathVariable Long postId) {
        postService.incrementLikeCount(postId);
        return ResponseEntity.ok(WrapRes.success("Like count incremented"));
    }

    @Operation(summary = "Tăng comment count", description = "Tăng số lượt comment của bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Tăng comment count thành công")
    })
    @PostMapping("/{postId}/comment-count")
    public ResponseEntity<WrapRes<?>> incrementCommentCount(@PathVariable Long postId) {
        postService.incrementCommentCount(postId);
        return ResponseEntity.ok(WrapRes.success("Comment count incremented"));
    }

    @Operation(summary = "Tăng share count", description = "Tăng số lượt share của bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Tăng share count thành công")
    })
    @PostMapping("/{postId}/share-count")
    public ResponseEntity<WrapRes<?>> incrementShareCount(@PathVariable Long postId) {
        postService.incrementShareCount(postId);
        return ResponseEntity.ok(WrapRes.success("Share count incremented"));
    }

    @Operation(summary = "Tăng view count", description = "Tăng số lượt xem của bài viết", responses = {
            @ApiResponse(responseCode = "200", description = "Tăng view count thành công")
    })
    @PostMapping("/{postId}/view")
    public ResponseEntity<WrapRes<?>> incrementViewCount(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
        return ResponseEntity.ok(WrapRes.success("View count incremented"));
    }
    
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
                // Log warning if mapping fails
            }
        }
        
        return MediaResponse.builder()
            .id(media.getId())
            .mediaUrl(media.getMediaUrl())
            .mediaType(media.getMediaType())
            .originalFilename(media.getOriginalFilename())
            .fileSize(media.getFileSize())
            .uploadedBy(uploadedByResponse)
            .createdAt(media.getCreatedAt())
            .updatedAt(media.getUpdatedAt())
            .build();
    }
} 