package com.nhom4.xoxo.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.service.NotificationService;
import com.nhom4.xoxo.dto.req.UpdateUserRequest;
import com.nhom4.xoxo.dto.res.SearchResultResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Notification;
import com.nhom4.xoxo.entity.User;

import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.SearchService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final SearchService searchService;

    @Operation(summary = "Lấy thông tin cá nhân của user hiện tại", description = "Yêu cầu đã đăng nhập. Trả về thông tin user.", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin user thành công")
    })
    @GetMapping("/profile")
    public ResponseEntity<WrapRes<?>> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        userResponse.setAvatarUrl(cloudinaryService.buildCloudinaryUrl(user.getAvatarUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE));
        userResponse.setCoverUrl(cloudinaryService.buildCloudinaryUrl(user.getCoverUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE));
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    @Operation(summary = "Cập nhật thông tin cá nhân của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật thông tin user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật thông tin user thành công")
    })
    @PutMapping("/profile")
    public ResponseEntity<WrapRes<?>> updateUserProfile(@RequestBody @Valid UpdateUserRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        modelMapper.map(updateRequest, user);
        User updatedUser = userService.updateUser(user, user);
        UserResponse userResponse = modelMapper.map(updatedUser, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    @Operation(summary = "Cập nhật ảnh đại diện của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật ảnh đại diện của user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật ảnh đại diện của user thành công")
    })
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File quá lớn! Vui lòng chọn ảnh nhỏ hơn 2MB.");
        }
        
        // Sử dụng method mới để lấy trực tiếp URL
        String avatarUrl = cloudinaryService.uploadImageAndGetUrl(file, "avatars");
        userService.updateAvatar(user, avatarUrl);
        
        return ResponseEntity.ok(WrapRes.success("Avatar updated successfully"));
    }

    @Operation(summary = "Cập nhật ảnh bìa của user hiện tại", description = "Yêu cầu đã đăng nhập. Cập nhật ảnh bìa của user.", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật ảnh bìa của user thành công")
    })
    @PostMapping(value = "/profile/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCover(@RequestParam("file") MultipartFile file, Principal principal) {
        String email = principal.getName();
        User user = userService.findByEmail(email);
        long maxSize = 2 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File quá lớn! Vui lòng chọn ảnh nhỏ hơn 2MB.");
        }
        
        // Sử dụng method mới để lấy trực tiếp URL
        String coverUrl = cloudinaryService.uploadImageAndGetUrl(file, "covers");
        userService.updateCover(user, coverUrl);
        
        return ResponseEntity.ok(WrapRes.success("Cover updated successfully"));
    }

    @Operation(summary = "Lấy thông tin user theo username", description = "Lấy thông tin user theo username", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy thông tin user theo username thành công")
    })
    @GetMapping("/{username}")
    public ResponseEntity<WrapRes<?>> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            UserResponse userResponse = modelMapper.map(user.get(), UserResponse.class);
            userResponse.setAvatarUrl(cloudinaryService.buildCloudinaryUrl(user.get().getAvatarUrl(),com.nhom4.xoxo.enums.MediaType.IMAGE));
            userResponse.setCoverUrl(cloudinaryService.buildCloudinaryUrl(user.get().getCoverUrl(), com.nhom4.xoxo.enums.MediaType.IMAGE));
            return ResponseEntity.ok(WrapRes.success(userResponse));
        }
        return ResponseEntity.ok(WrapRes.success(user));
    }

    // ==================== NOTIFICATION ENDPOINTS ====================

    @Operation(summary = "Lấy danh sách notifications của user hiện tại", description = "Lấy notifications có phân trang", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy notifications thành công")
    })
    @GetMapping("/notifications")
    public ResponseEntity<WrapRes<?>> myNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var user = userService.findByEmail(principal.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUserNotifications(user.getId(), pageable);

        return ResponseEntity.ok(WrapRes.success(notifications));
    }

    @Operation(summary = "Lấy danh sách notifications chưa đọc", description = "Lấy tất cả notifications chưa đọc của user", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy notifications chưa đọc thành công")
    })
    @GetMapping("/notifications/unread")
    public ResponseEntity<WrapRes<?>> myUnreadNotifications(Principal principal) {
        var user = userService.findByEmail(principal.getName());
        List<Notification> unreadNotifications = notificationService.getUserUnreadNotifications(user.getId());

        return ResponseEntity.ok(WrapRes.success(unreadNotifications));
    }

    @Operation(summary = "Đếm số notifications chưa đọc", description = "Trả về số lượng notifications chưa đọc", responses = {
            @ApiResponse(responseCode = "200", description = "Đếm notifications thành công")
    })
    @GetMapping("/notifications/unread/count")
    public ResponseEntity<WrapRes<?>> myUnreadNotificationsCount(Principal principal) {
        var user = userService.findByEmail(principal.getName());
        Long count = notificationService.countUserUnreadNotifications(user.getId());

        return ResponseEntity.ok(WrapRes.success(count));
    }

    @Operation(summary = "Đánh dấu notification đã đọc", description = "Đánh dấu một notification cụ thể là đã đọc", responses = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu đã đọc thành công")
    })
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<WrapRes<?>> markNotificationAsRead(
            @PathVariable Long id,
            Principal principal) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.ok(WrapRes.success("Notification marked as read"));
    }

    @Operation(summary = "Đánh dấu tất cả notifications đã đọc", description = "Đánh dấu tất cả notifications của user là đã đọc", responses = {
            @ApiResponse(responseCode = "200", description = "Đánh dấu tất cả đã đọc thành công")
    })
    @PutMapping("/notifications/read-all")
    public ResponseEntity<WrapRes<?>> markAllNotificationsAsRead(Principal principal) {
        var user = userService.findByEmail(principal.getName());
        notificationService.markAllUserNotificationsAsRead(user.getId());
        return ResponseEntity.ok(WrapRes.success("All notifications marked as read"));
    }

    @Operation(summary = "Lấy notifications theo loại", description = "Lấy notifications theo loại cụ thể", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy notifications theo loại thành công")
    })
    @GetMapping("/notifications/type/{type}")
    public ResponseEntity<WrapRes<?>> getNotificationsByType(
            @PathVariable String type,
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // TODO: Implement notification filtering by type
        return ResponseEntity.ok(WrapRes.success("Feature coming soon"));
    }

    @Operation(summary = "Xóa notification", description = "Xóa một notification cụ thể", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa notification thành công")
    })
    @PutMapping("/notifications/{id}/delete")
    public ResponseEntity<WrapRes<?>> deleteNotification(
            @PathVariable Long id,
            Principal principal) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(WrapRes.success("Notification deleted successfully"));
    }

    // ==================== SEARCH ENDPOINTS ====================

    @Operation(summary = "Tìm kiếm tổng hợp", description = "Tìm kiếm trong tất cả User, Post, Group với từ khóa", responses = {
            @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    })
    @GetMapping("/search")
    public ResponseEntity<WrapRes<SearchResultResponse>> searchAll(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        SearchResultResponse result = searchService.searchAll(keyword, pageable);
        return ResponseEntity.ok(WrapRes.success(result));
    }

    @Operation(summary = "Tìm kiếm users", description = "Tìm kiếm users theo từ khóa", responses = {
            @ApiResponse(responseCode = "200", description = "Tìm kiếm users thành công")
    })
    @GetMapping("/search/users")
    public ResponseEntity<WrapRes<SearchResultResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        SearchResultResponse result = searchService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(WrapRes.success(result));
    }

    @Operation(summary = "Tìm kiếm posts", description = "Tìm kiếm posts theo từ khóa", responses = {
            @ApiResponse(responseCode = "200", description = "Tìm kiếm posts thành công")
    })
    @GetMapping("/search/posts")
    public ResponseEntity<WrapRes<SearchResultResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        SearchResultResponse result = searchService.searchPosts(keyword, pageable);
        return ResponseEntity.ok(WrapRes.success(result));
    }

    @Operation(summary = "Tìm kiếm groups", description = "Tìm kiếm groups theo từ khóa", responses = {
            @ApiResponse(responseCode = "200", description = "Tìm kiếm groups thành công")
    })
    @GetMapping("/search/groups")
    public ResponseEntity<WrapRes<SearchResultResponse>> searchGroups(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        SearchResultResponse result = searchService.searchGroups(keyword, pageable);
        return ResponseEntity.ok(WrapRes.success(result));
    }
}