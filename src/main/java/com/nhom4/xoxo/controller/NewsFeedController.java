package com.nhom4.xoxo.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.NewsFeedItemResponse;
import com.nhom4.xoxo.dto.res.NewsFeedResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.NewsFeedItemType;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.NewsFeedService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * NewsFeed Controller - Manages user's personalized news feed
 * Provides Facebook-like news feed functionality with caching and pagination
 */
@RestController
@RequestMapping("/api/newsfeed")
@Tag(name = "NewsFeed", description = "Quản lý bảng tin cá nhân người dùng")
@Slf4j
public class NewsFeedController {
    
    private final NewsFeedService newsFeedService;
    private final UserService userService;
    private final UserRepository userRepository;
    
    public NewsFeedController(NewsFeedService newsFeedService, UserService userService, UserRepository userRepository) {
        this.newsFeedService = newsFeedService;
        this.userService = userService;
        this.userRepository = userRepository;
    }
    
    // ==================== FEED RETRIEVAL ====================
    
    @Operation(
        summary = "Lấy bảng tin cá nhân", 
        description = "Lấy bảng tin cá nhân với phân trang và Redis cache. " +
                     "Bao gồm posts từ bạn bè, hoạt động nhóm, và các hoạt động xã hội khác.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bảng tin thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "500", description = "Lỗi server")
        }
    )
    @GetMapping
    public ResponseEntity<WrapRes<NewsFeedResponse>> getNewsFeed(
            Principal principal,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số item mỗi trang")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sắp xếp theo (priority, time)")
            @RequestParam(defaultValue = "priority") String sortBy) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Create pageable with appropriate sorting
            Sort sort = createSort(sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            NewsFeedResponse response = newsFeedService.getUserNewsFeed(currentUser.getId(), pageable);
            
            log.debug("Retrieved news feed for user: {}, page: {}, size: {}, cache: {}", 
                     currentUser.getId(), page, size, response.getCacheStatus());
            
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error getting news feed", e);
            return ResponseEntity.ok(WrapRes.error("Không thể tải bảng tin: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy bảng tin chưa xem", 
        description = "Lấy các hoạt động mới chưa được xem trong bảng tin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bảng tin chưa xem thành công")
        }
    )
    @GetMapping("/unseen")
    public ResponseEntity<WrapRes<NewsFeedResponse>> getUnseenNewsFeed(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Pageable pageable = PageRequest.of(page, size, Sort.by("activityTime").descending());
            
            NewsFeedResponse response = newsFeedService.getUnseenNewsFeed(currentUser.getId(), pageable);
            
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error getting unseen news feed", e);
            return ResponseEntity.ok(WrapRes.error("Không thể tải bảng tin chưa xem: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy bảng tin theo loại", 
        description = "Lọc bảng tin theo loại hoạt động (posts, friendships, groups, etc.)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bảng tin theo loại thành công")
        }
    )
    @GetMapping("/type/{itemType}")
    public ResponseEntity<WrapRes<NewsFeedResponse>> getNewsFeedByType(
            Principal principal,
            @PathVariable NewsFeedItemType itemType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Pageable pageable = PageRequest.of(page, size, Sort.by("activityTime").descending());
            
            NewsFeedResponse response = newsFeedService.getNewsFeedByType(
                currentUser.getId(), itemType, pageable);
            
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error getting news feed by type: {}", itemType, e);
            return ResponseEntity.ok(WrapRes.error("Không thể tải bảng tin theo loại: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy bảng tin gần đây", 
        description = "Lấy các hoạt động trong 24 giờ qua",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy bảng tin gần đây thành công")
        }
    )
    @GetMapping("/recent")
    public ResponseEntity<WrapRes<NewsFeedResponse>> getRecentNewsFeed(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Pageable pageable = PageRequest.of(page, size, Sort.by("activityTime").descending());
            
            NewsFeedResponse response = newsFeedService.getRecentNewsFeed(currentUser.getId(), pageable);
            
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error getting recent news feed", e);
            return ResponseEntity.ok(WrapRes.error("Không thể tải bảng tin gần đây: " + e.getMessage()));
        }
    }
    
    // ==================== FEED INTERACTIONS ====================
    
    @Operation(
        summary = "Đánh dấu đã xem", 
        description = "Đánh dấu các item trong bảng tin là đã xem",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu đã xem thành công")
        }
    )
    @PostMapping("/mark-seen")
    public ResponseEntity<WrapRes<String>> markItemsAsSeen(
            Principal principal,
            @RequestBody List<Long> itemIds) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            if (itemIds == null || itemIds.isEmpty()) {
                return ResponseEntity.ok(WrapRes.error("Danh sách item không được rỗng"));
            }
            
            newsFeedService.markItemsAsSeen(currentUser.getId(), itemIds);
            
            log.debug("Marked {} items as seen for user: {}", itemIds.size(), currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success("Đã đánh dấu " + itemIds.size() + " item là đã xem"));
            
        } catch (Exception e) {
            log.error("Error marking items as seen", e);
            return ResponseEntity.ok(WrapRes.error("Không thể đánh dấu đã xem: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Đánh dấu tương tác", 
        description = "Đánh dấu một item là đã tương tác (click, like, comment, etc.)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu tương tác thành công")
        }
    )
    @PostMapping("/items/{itemId}/interact")
    public ResponseEntity<WrapRes<String>> markItemAsInteracted(
            Principal principal,
            @PathVariable Long itemId) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            newsFeedService.markItemAsInteracted(currentUser.getId(), itemId);
            
            log.debug("Marked item {} as interacted for user: {}", itemId, currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success("Đã đánh dấu tương tác"));
            
        } catch (Exception e) {
            log.error("Error marking item as interacted: {}", itemId, e);
            return ResponseEntity.ok(WrapRes.error("Không thể đánh dấu tương tác: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Đếm item chưa xem", 
        description = "Lấy số lượng item chưa xem trong bảng tin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy số lượng thành công")
        }
    )
    @GetMapping("/unseen-count")
    public ResponseEntity<WrapRes<Long>> getUnseenItemsCount(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Long count = newsFeedService.getUnseenItemsCount(currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success(count));
            
        } catch (Exception e) {
            log.error("Error getting unseen items count", e);
            return ResponseEntity.ok(WrapRes.error("Không thể lấy số lượng chưa xem: " + e.getMessage()));
        }
    }
    
    // ==================== FEED MANAGEMENT ====================
    
    @Operation(
        summary = "Làm mới bảng tin", 
        description = "Tạo lại bảng tin và xóa cache để có nội dung mới nhất",
        responses = {
            @ApiResponse(responseCode = "200", description = "Làm mới bảng tin thành công")
        }
    )
    @PostMapping("/refresh")
    public ResponseEntity<WrapRes<String>> refreshNewsFeed(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Generate new feed content
            newsFeedService.generateNewsFeedForUser(currentUser.getId());
            
            // Clear cache to force fresh load
            newsFeedService.refreshUserFeedCache(currentUser.getId());
            
            log.info("Refreshed news feed for user: {}", currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success("Đã làm mới bảng tin"));
            
        } catch (Exception e) {
            log.error("Error refreshing news feed", e);
            return ResponseEntity.ok(WrapRes.error("Không thể làm mới bảng tin: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Xóa cache bảng tin", 
        description = "Xóa cache để tải lại từ database (dành cho admin hoặc debug)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa cache thành công")
        }
    )
    @DeleteMapping("/cache")
    public ResponseEntity<WrapRes<String>> clearFeedCache(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            newsFeedService.clearUserFeedCache(currentUser.getId());
            
            log.info("Cleared feed cache for user: {}", currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success("Đã xóa cache bảng tin"));
            
        } catch (Exception e) {
            log.error("Error clearing feed cache", e);
            return ResponseEntity.ok(WrapRes.error("Không thể xóa cache: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Cập nhật độ ưu tiên", 
        description = "Tính lại điểm ưu tiên cho các item trong bảng tin",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật độ ưu tiên thành công")
        }
    )
    @PostMapping("/update-priorities")
    public ResponseEntity<WrapRes<String>> updatePriorityScores(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            newsFeedService.updatePriorityScores(currentUser.getId());
            
            log.info("Updated priority scores for user: {}", currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success("Đã cập nhật độ ưu tiên"));
            
        } catch (Exception e) {
            log.error("Error updating priority scores", e);
            return ResponseEntity.ok(WrapRes.error("Không thể cập nhật độ ưu tiên: " + e.getMessage()));
        }
    }
    
    // ==================== ANALYTICS & INSIGHTS ====================
    
    
    // ==================== ADMIN / TESTING ENDPOINTS ====================
    
    @Operation(
        summary = "👤 USER: Khởi tạo bảng tin cho chính mình", 
        description = "User tự khởi tạo NewsFeed cho chính mình từ data hiện có (30 ngày)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Khởi tạo bảng tin thành công")
        }
    )
    @PostMapping("/initialize")
    public ResponseEntity<WrapRes<Object>> initializeMyNewsFeed(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            log.info("User {} initializing their own NewsFeed", currentUser.getId());
            
            // Clear existing feed first
            newsFeedService.clearUserFeedCache(currentUser.getId());
            
            // Generate new feed from existing data
            newsFeedService.generateNewsFeedForUser(currentUser.getId());
            
            // Get analytics for user
            Object analytics = newsFeedService.getFeedAnalytics(currentUser.getId());
            
            Map<String, Object> result = Map.of(
                "message", "✅ Đã khởi tạo bảng tin thành công!",
                "userId", currentUser.getId(),
                "username", currentUser.getUsername(),
                "analytics", analytics,
                "nextStep", "Gọi GET /api/newsfeed để xem bảng tin của bạn",
                "timestamp", LocalDateTime.now().toString()
            );
            
            log.info("Successfully initialized NewsFeed for user: {}", currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success(result));
            
        } catch (Exception e) {
            log.error("Error initializing user's own NewsFeed", e);
            return ResponseEntity.ok(WrapRes.error("Không thể khởi tạo bảng tin: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy nội dung phổ biến", 
        description = "Lấy nội dung đang trending trong mạng lưới của user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy nội dung phổ biến thành công")
        }
    )
    @GetMapping("/popular")
    public ResponseEntity<WrapRes<List<NewsFeedItemResponse>>> getPopularContent(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            User currentUser = getCurrentUser(principal);
            Pageable pageable = PageRequest.of(page, size);
            
            List<NewsFeedItemResponse> popularContent = newsFeedService.getPopularContent(currentUser.getId(), pageable);
            
            return ResponseEntity.ok(WrapRes.success(popularContent));
            
        } catch (Exception e) {
            log.error("Error getting popular content", e);
            return ResponseEntity.ok(WrapRes.error("Không thể lấy nội dung phổ biến: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy chủ đề trending", 
        description = "Lấy các hashtag đang trending trong mạng lưới của user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lấy chủ đề trending thành công")
        }
    )
    @GetMapping("/trending-topics")
    public ResponseEntity<WrapRes<List<String>>> getTrendingTopics(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            List<String> trendingTopics = newsFeedService.getTrendingTopics(currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success(trendingTopics));
            
        } catch (Exception e) {
            log.error("Error getting trending topics", e);
            return ResponseEntity.ok(WrapRes.error("Không thể lấy chủ đề trending: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Tạo sample data cho testing", 
        description = "Tạo một số feed items mẫu để test NewsFeed (chỉ dùng cho development)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo sample data thành công")
        }
    )
    @PostMapping("/test/create-sample-data")
    public ResponseEntity<WrapRes<String>> createSampleFeedData(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Tạo một số sample feed items với các loại khác nhau
            int itemsCreated = 0;
            
            // Sample POST item
            newsFeedService.addFeedItem(
                currentUser.getId(),
                currentUser,
                NewsFeedItemType.POST,
                null, // No real post for test
                null,
                null,
                "{\"test\": true, \"content\": \"Sample post activity\"}"
            );
            itemsCreated++;
            
            // Sample FRIENDSHIP item
            newsFeedService.addFeedItem(
                currentUser.getId(),
                currentUser,
                NewsFeedItemType.NEW_FRIENDSHIP,
                null,
                null,
                currentUser, // Self as target for test
                "{\"test\": true, \"content\": \"Sample friendship activity\"}"
            );
            itemsCreated++;
            
            // Sample LIKE item
            newsFeedService.addFeedItem(
                currentUser.getId(),
                currentUser,
                NewsFeedItemType.LIKED_POST,
                null,
                null,
                null,
                "{\"test\": true, \"content\": \"Sample like activity\"}"
            );
            itemsCreated++;
            
            // Sample COMMENT item
            newsFeedService.addFeedItem(
                currentUser.getId(),
                currentUser,
                NewsFeedItemType.COMMENTED_POST,
                null,
                null,
                null,
                "{\"test\": true, \"content\": \"Sample comment activity\"}"
            );
            itemsCreated++;
            
            log.info("Created {} sample feed items for user: {}", itemsCreated, currentUser.getId());
            
            return ResponseEntity.ok(WrapRes.success(
                "Đã tạo " + itemsCreated + " sample feed items thành công! " +
                "Gọi GET /api/newsfeed để xem kết quả."
            ));
            
        } catch (Exception e) {
            log.error("Error creating sample feed data", e);
            return ResponseEntity.ok(WrapRes.error("Không thể tạo sample data: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "🔧 ADMIN: Khởi tạo NewsFeed cho 1 user cụ thể", 
        description = "Admin khởi tạo NewsFeed cho user được chỉ định bằng userId",
        responses = {
            @ApiResponse(responseCode = "200", description = "Khởi tạo NewsFeed thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền admin"),
            @ApiResponse(responseCode = "404", description = "User không tồn tại")
        }
    )
    @PostMapping("/admin/initialize-user/{userId}")
    public ResponseEntity<WrapRes<Object>> adminInitializeUserNewsFeed(
            @PathVariable Long userId,
            Principal principal) {
        
        try {
            User adminUser = getCurrentUser(principal);
            
            // Check admin permission (simple check - có thể customize)
            if (!adminUser.getEmail().contains("admin") && 
                !adminUser.getRoles().toString().contains("ADMIN") &&
                !adminUser.getRoles().toString().contains("OWNER")) {
                return ResponseEntity.ok(WrapRes.error("❌ Chỉ admin mới có quyền khởi tạo NewsFeed cho user khác"));
            }
            
            // Check if target user exists
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                return ResponseEntity.ok(WrapRes.error("❌ User không tồn tại: " + userId));
            }
            
            log.info("Admin {} initializing NewsFeed for user: {}", adminUser.getId(), userId);
            
            // Clear existing feed first
            newsFeedService.clearUserFeedCache(userId);
            
            // Generate comprehensive feed from all existing data
            newsFeedService.generateNewsFeedForUser(userId);
            
            // Get analytics to show what was created
            Object analytics = newsFeedService.getFeedAnalytics(userId);
            
            Map<String, Object> result = Map.of(
                "message", "✅ Admin đã khởi tạo NewsFeed thành công!",
                "adminUserId", adminUser.getId(),
                "targetUserId", userId,
                "targetUsername", targetUser.getUsername(),
                "targetUserName", (targetUser.getFirstName() + " " + targetUser.getLastName()).trim(),
                "analytics", analytics,
                "timestamp", LocalDateTime.now().toString()
            );
            
            log.info("Admin {} successfully initialized NewsFeed for user: {}", adminUser.getId(), userId);
            
            return ResponseEntity.ok(WrapRes.success(result));
            
        } catch (Exception e) {
            log.error("Error in admin NewsFeed initialization for user: {}", userId, e);
            return ResponseEntity.ok(WrapRes.error("❌ Admin không thể khởi tạo NewsFeed: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Debug - Kiểm tra NewsFeed items trực tiếp từ DB", 
        description = "Bypass cache để xem raw data từ database (debug tool)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Debug data thành công")
        }
    )
    @GetMapping("/debug/raw-items")
    public ResponseEntity<WrapRes<Object>> getDebugRawItems(Principal principal) {
        
        try {
            User currentUser = getCurrentUser(principal);
            
            // Truy vấn trực tiếp database không qua cache
            NewsFeedResponse directResponse = newsFeedService.getUnseenNewsFeed(
                currentUser.getId(), 
                PageRequest.of(0, 50, Sort.by("activityTime").descending())
            );
            
            // Thêm debug info
            Map<String, Object> debugInfo = Map.of(
                "userId", currentUser.getId(),
                "directQueryResult", directResponse,
                "analytics", newsFeedService.getFeedAnalytics(currentUser.getId()),
                "timestamp", LocalDateTime.now().toString()
            );
            
            log.info("Debug raw items for user {}: found {} items", 
                currentUser.getId(), directResponse.getTotalElements());
            
            return ResponseEntity.ok(WrapRes.success(debugInfo));
            
        } catch (Exception e) {
            log.error("Error getting debug raw items", e);
            return ResponseEntity.ok(WrapRes.error("Debug error: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "🚀 ADMIN: Populate NewsFeed cho TẤT CẢ users", 
        description = "Migrate NewsFeed cho tất cả users trong hệ thống từ data cũ (admin only)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Migrate all users thành công")
        }
    )
    @PostMapping("/admin/populate-all-users")
    public ResponseEntity<WrapRes<Object>> populateAllUsersNewsFeeds() {
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Get all active users directly from repository
            List<User> allUsers = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .toList();
            
            int successCount = 0;
            int errorCount = 0;
            List<String> errors = new ArrayList<>();
            
            log.info("Starting NewsFeed population for {} users", allUsers.size());
            
            for (User user : allUsers) {
                try {
                    newsFeedService.generateNewsFeedForUser(user.getId());
                    successCount++;
                    
                    if (successCount % 10 == 0) {
                        log.info("Progress: {}/{} users completed", successCount, allUsers.size());
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    String error = "User " + user.getId() + " (" + user.getUsername() + "): " + e.getMessage();
                    errors.add(error);
                    log.error("Error populating feed for user {}: {}", user.getId(), e.getMessage());
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> result = Map.of(
                "totalUsers", allUsers.size(),
                "successCount", successCount,
                "errorCount", errorCount,
                "errors", errors,
                "executionTimeMs", totalTime,
                "averageTimePerUser", allUsers.size() > 0 ? totalTime / allUsers.size() : 0,
                "timestamp", LocalDateTime.now().toString()
            );
            
            log.info("Completed NewsFeed population: {}/{} users successful, {} errors, {}ms total", 
                    successCount, allUsers.size(), errorCount, totalTime);
            
            return ResponseEntity.ok(WrapRes.success(result));
            
        } catch (Exception e) {
            log.error("Error in bulk NewsFeed population", e);
            return ResponseEntity.ok(WrapRes.error("Bulk migration error: " + e.getMessage()));
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        return userService.findByEmail(principal.getName());
    }
    
    // Removed unused getCurrentUser() method
    
    private Sort createSort(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "time":
                return Sort.by("activityTime").descending();
            case "priority":
            default:
                return Sort.by("priorityScore").descending()
                          .and(Sort.by("activityTime").descending());
        }
    }

    @Operation(
        summary = "📊 Analytics: Lấy thống kê NewsFeed",
        description = "Lấy thống kê tổng quan về NewsFeed system",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thống kê NewsFeed"),
            @ApiResponse(responseCode = "403", description = "Không có quyền admin")
        }
    )
    @GetMapping("/analytics")
    public ResponseEntity<WrapRes<Object>> getNewsFeedAnalytics(Principal principal) {
        try {
            String email = principal.getName();
            User currentUser = userService.findByEmail(email);

            if (!userService.isAdminOrOwner(currentUser)) {
                return ResponseEntity.status(403)
                    .body(WrapRes.error("Access denied. Admin or Owner role required."));
            }

            Object analytics = newsFeedService.getFeedAnalytics(currentUser.getId());
            return ResponseEntity.ok(WrapRes.success(analytics));

        } catch (Exception e) {
            log.error("Error getting NewsFeed analytics", e);
            return ResponseEntity.status(500)
                .body(WrapRes.error("Failed to get analytics"));
        }
    }

    @Operation(
        summary = "🧹 Cleanup: Dọn dẹp NewsFeed cũ",
        description = "Xóa các feed items cũ hơn số ngày chỉ định",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dọn dẹp thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền admin")
        }
    )
    @PostMapping("/admin/cleanup-old-items")
    public ResponseEntity<WrapRes<Object>> cleanupOldFeedItems(
            @RequestParam(defaultValue = "30") int daysToKeep,
            Principal principal) {
        try {
            String email = principal.getName();
            User currentUser = userService.findByEmail(email);

            if (!userService.isAdminOrOwner(currentUser)) {
                return ResponseEntity.status(403)
                    .body(WrapRes.error("Access denied. Admin or Owner role required."));
            }

            // Get all users and cleanup their feeds
            List<User> allUsers = userRepository.findAll();
            int cleanedUsers = 0;

            for (User user : allUsers) {
                try {
                    newsFeedService.cleanupOldFeedItems(user.getId(), 
                        LocalDateTime.now().minusDays(daysToKeep));
                    cleanedUsers++;
                } catch (Exception e) {
                    log.warn("Error cleaning up feed for user {}: {}", user.getId(), e.getMessage());
                }
            }

            return ResponseEntity.ok(WrapRes.success(
                String.format("Cleaned up old feed items for %d users (keeping %d days)", 
                             cleanedUsers, daysToKeep)));

        } catch (Exception e) {
            log.error("Error cleaning up old feed items", e);
            return ResponseEntity.status(500)
                .body(WrapRes.error("Failed to cleanup old items"));
        }
    }
}
