# NewsFeed Integration Guide

## Cách tích hợp NewsFeed với các controller khác

### 1. Integration với PostController

Để tự động thêm feed items khi có posts mới, thêm code sau vào PostController:

```java
@RestController
@RequestMapping("/api/posts")
public class PostController {
    
    private final PostService postService;
    private final NewsFeedIntegrationService newsFeedIntegrationService; // Thêm dependency này
    
    @PostMapping
    public ResponseEntity<WrapRes<?>> createPost(@RequestBody @Valid PostRequest postRequest) {
        // ... existing code để tạo post ...
        
        Post createdPost = postService.createPost(post);
        
        // *** THÊM CODE NÀY ĐỂ TỰ ĐỘNG TẠO FEED ITEMS ***
        try {
            newsFeedIntegrationService.onPostCreated(createdPost);
        } catch (Exception e) {
            log.warn("Failed to create news feed items for post: {}", createdPost.getId(), e);
            // Không throw error vì post đã tạo thành công
        }
        
        // ... rest of existing code ...
        return ResponseEntity.ok(WrapRes.success(res));
    }
}
```

### 2. Integration với PostReactionController

```java
@PostMapping("/{postId}/like")
public ResponseEntity<WrapRes<?>> likePost(@PathVariable Long postId, Principal principal) {
    // ... existing code ...
    
    Post post = postService.getPostById(postId).orElseThrow();
    User user = userService.findByEmail(principal.getName());
    
    // Existing like logic...
    
    // *** THÊM CODE NÀY ***
    try {
        newsFeedIntegrationService.onPostLiked(post, user);
    } catch (Exception e) {
        log.warn("Failed to create news feed item for like: postId={}, userId={}", postId, user.getId(), e);
    }
    
    return ResponseEntity.ok(WrapRes.success("Liked successfully"));
}

@PostMapping("/{postId}/comment")
public ResponseEntity<WrapRes<?>> commentPost(@PathVariable Long postId, @RequestBody CommentRequest request, Principal principal) {
    // ... existing code ...
    
    Post post = postService.getPostById(postId).orElseThrow();
    User user = userService.findByEmail(principal.getName());
    
    // Existing comment logic...
    
    // *** THÊM CODE NÀY ***
    try {
        newsFeedIntegrationService.onPostCommented(post, user);
    } catch (Exception e) {
        log.warn("Failed to create news feed item for comment: postId={}, userId={}", postId, user.getId(), e);
    }
    
    return ResponseEntity.ok(WrapRes.success(commentResponse));
}
```

### 3. Integration với FriendshipController

```java
@PostMapping("/{friendshipId}/accept")
public ResponseEntity<WrapRes<?>> acceptFriendship(@PathVariable Long friendshipId, Principal principal) {
    // ... existing code ...
    
    Friendship friendship = friendshipService.acceptFriendship(friendshipId, currentUser.getId());
    
    // *** THÊM CODE NÀY ***
    try {
        newsFeedIntegrationService.onFriendshipCreated(friendship.getUser(), friendship.getFriend());
    } catch (Exception e) {
        log.warn("Failed to create news feed items for friendship: {}", friendshipId, e);
    }
    
    return ResponseEntity.ok(WrapRes.success(friendshipResponse));
}
```

### 4. Clean up khi xóa posts

```java
@DeleteMapping("/{postId}")
public ResponseEntity<WrapRes<?>> deletePost(@PathVariable Long postId, Principal principal) {
    // ... existing validation ...
    
    postService.deletePost(postId);
    
    // *** THÊM CODE NÀY ĐỂ CLEAN UP FEED ITEMS ***
    try {
        newsFeedIntegrationService.onPostDeleted(postId);
    } catch (Exception e) {
        log.warn("Failed to clean up news feed items for deleted post: {}", postId, e);
    }
    
    return ResponseEntity.ok(WrapRes.success("Post deleted successfully"));
}
```

## API Endpoints của NewsFeed

### Main Endpoints
- `GET /api/newsfeed` - Lấy bảng tin chính (có cache)
- `GET /api/newsfeed/unseen` - Bảng tin chưa xem  
- `GET /api/newsfeed/type/{type}` - Lọc theo loại hoạt động
- `GET /api/newsfeed/recent` - Hoạt động trong 24h qua

### Interaction Endpoints
- `POST /api/newsfeed/mark-seen` - Đánh dấu đã xem
- `POST /api/newsfeed/items/{itemId}/interact` - Đánh dấu tương tác
- `GET /api/newsfeed/unseen-count` - Đếm số item chưa xem

### Management Endpoints
- `POST /api/newsfeed/generate` - Tạo feed ban đầu cho user mới
- `POST /api/newsfeed/refresh` - Làm mới bảng tin
- `DELETE /api/newsfeed/cache` - Xóa cache
- `POST /api/newsfeed/update-priorities` - Cập nhật độ ưu tiên

### Analytics Endpoints
- `GET /api/newsfeed/analytics` - Thống kê bảng tin
- `GET /api/newsfeed/popular` - Nội dung phổ biến
- `GET /api/newsfeed/trending-topics` - Hashtags trending

## Workflow khuyến nghị

1. **User đăng ký mới**: Gọi `POST /api/newsfeed/generate`
2. **User đăng nhập**: Gọi `GET /api/newsfeed/unseen-count` để hiển thị badge
3. **User xem feed**: Gọi `GET /api/newsfeed?page=0&size=20`
4. **User scroll xuống**: Gọi `POST /api/newsfeed/mark-seen` với list IDs đã xem
5. **Định kỳ**: Gọi `POST /api/newsfeed/refresh` để có nội dung mới

## Cache Strategy

- **Feed chính**: Cache 30 phút với Redis
- **Unseen count**: Cache 5 phút
- **Popular content**: Cache 20 phút  
- **Analytics**: Cache 60 phút

Cache sẽ tự động clear khi có hoạt động mới của user hoặc bạn bè.

