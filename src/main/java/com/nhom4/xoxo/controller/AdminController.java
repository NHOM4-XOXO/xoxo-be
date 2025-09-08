package com.nhom4.xoxo.controller;

import java.util.List;
import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.AdminGroupStatusRequest;
import com.nhom4.xoxo.dto.req.AdminReportReviewRequest;
import com.nhom4.xoxo.dto.req.TogglePostStatusRequest;
import com.nhom4.xoxo.dto.req.ToggleUserStatusRequest;
import com.nhom4.xoxo.dto.res.GroupAnalyticsResponse;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.dto.res.PostResponse;
import com.nhom4.xoxo.dto.res.ReportAnalyticsResponse;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.service.GroupService;
import com.nhom4.xoxo.service.PostService;
import com.nhom4.xoxo.service.ReportService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;
    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ModelMapper modelMapper;

    @Operation(summary = "Lấy danh sách tất cả user", description = "Chỉ ADMIN hoặc OWNER mới có quyền truy cập.")
    @ApiResponse(responseCode = "200", description = "Danh sách user")
    @GetMapping("/users")
    public ResponseEntity<WrapRes<List<UserResponseProjection>>> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        List<UserResponseProjection> users;
        if (userService.isAdminOrOwner(currentUser)) {
            if (currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN))) {
                users = userService.findAllUsersAdmin();
            } else {
                users = userService.findAllUsersOwner();
            }
        } else {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        return ResponseEntity.ok(WrapRes.success(users));
    }

    @Operation(summary = "Xóa user theo id", description = "Chỉ ADMIN hoặc OWNER mới có quyền xóa user. Không được xóa chính mình."

    )
    @ApiResponse(responseCode = "200", description = "Xóa user thành công")
    @ApiResponse(responseCode = "400", description = "Không được xóa chính mình")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<WrapRes<?>> deleteUser(
            @Parameter(description = "ID của user cần xóa") @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!userService.canDeleteUser(currentUser, userId)) {
            if (userService.isSelf(currentUser, userId)) {
                return ResponseEntity.badRequest()
                        .body(WrapRes.error(WrapResStatus.BAD_REQUEST, "Cannot delete your own account"));
            }
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        userService.deleteUser(userId, currentUser);
        return ResponseEntity.ok(WrapRes.success(Map.of("message", "User deleted successfully")));
    }

    @Operation(summary = "Bật/tắt trạng thái hoạt động của user", description = "Chỉ ADMIN hoặc OWNER mới có quyền cập nhật trạng thái user."

    )
    @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái user thành công")
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<WrapRes<?>> toggleUserStatus(
            @Parameter(description = "ID của user cần cập nhật trạng thái") @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Trạng thái enabled mới") @RequestBody ToggleUserStatusRequest statusRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!userService.canToggleUserStatus(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        Boolean enabled = statusRequest.getEnabled();
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(WrapRes.error(WrapResStatus.BAD_REQUEST, "enabled field is required"));
        }
        User updatedUser = userService.toggleUserStatus(userId, enabled, currentUser);
        UserResponse userResponse = modelMapper.map(updatedUser, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(Map.of(
                "message", "User status updated successfully",
                "user", userResponse)));
    }

    @Operation(summary = "Enable user bị disabled", description = "Chỉ ADMIN hoặc OWNER mới có quyền enable user.")
    @ApiResponse(responseCode = "200", description = "Enable user thành công")
    @PatchMapping("/users/{userId}/enable")
    public ResponseEntity<WrapRes<?>> enableUser(
            @Parameter(description = "ID của user cần enable") @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        
        if (!userService.canToggleUserStatus(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        
        User updatedUser = userService.toggleUserStatus(userId, true, currentUser);
        UserResponse userResponse = modelMapper.map(updatedUser, UserResponse.class);
        
        return ResponseEntity.ok(WrapRes.success(Map.of(
                "message", "User enabled successfully",
                "user", userResponse)));
    }

    @Operation(summary = "Lấy thông tin chi tiết user theo id", description = "Chỉ ADMIN, OWNER hoặc chính user đó mới có quyền xem thông tin chi tiết."

    )
    @ApiResponse(responseCode = "200", description = "Thông tin user")
    @ApiResponse(responseCode = "404", description = "User không tồn tại")
    @GetMapping("/users/{userId}")
    public ResponseEntity<WrapRes<?>> getUserById(
            @Parameter(description = "ID của user cần xem") @PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        User targetUser = userService.findById(userId, currentUser);
        if (targetUser == null) {
            return ResponseEntity.status(404).body(WrapRes.error(WrapResStatus.NOT_FOUND, "User not found"));
        }
        if (!userService.canViewUser(currentUser, userId)) {
            return ResponseEntity.status(403).body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied"));
        }
        UserResponse userResponse = modelMapper.map(targetUser, UserResponse.class);
        return ResponseEntity.ok(WrapRes.success(userResponse));
    }

    @Operation(summary = "Lấy danh sách tất cả bài viết", description = "Chỉ ADMIN hoặc OWNER mới có quyền truy cập.")
    @ApiResponse(responseCode = "200", description = "Danh sách bài viết")
    @GetMapping("/posts")
    public ResponseEntity<WrapRes<List<PostItemResponse>>> getAllPosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        List<PostItemResponse> posts = postService.getAllPosts();
        List<PostItemResponse> responses = posts.stream()
                .map(post -> modelMapper.map(post, PostItemResponse.class))
                .toList();
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @Operation(summary = "Xóa bài viết theo id", description = "Chỉ ADMIN hoặc OWNER mới có quyền xóa bài viết.")
    @ApiResponse(responseCode = "200", description = "Xóa bài viết thành công")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<WrapRes<?>> deletePost(
            @Parameter(description = "ID của post cần xóa") @PathVariable Long postId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        postService.deletePost(postId);
        return ResponseEntity.ok(WrapRes.success(Map.of("message", "Post deleted successfully")));
    }

    @Operation(summary = "Cập nhật trạng thái bài viết", description = "Chỉ ADMIN hoặc OWNER mới có quyền cập nhật trạng thái bài viết.")
    @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái bài viết thành công")
    @PostMapping("/posts/{postId}/status")
    public ResponseEntity<WrapRes<?>> togglePostStatus(
            @Parameter(description = "ID của post cần cập nhật trạng thái") @PathVariable Long postId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Trạng thái mới") @RequestBody TogglePostStatusRequest statusRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }
        PostStatus status = statusRequest.getStatus();
        if (status == null) {
            return ResponseEntity.badRequest()
                    .body(WrapRes.error(WrapResStatus.BAD_REQUEST, "status field is required"));
        }
        postService.updatePostStatus(postId, status);
        Post updatedPost = postRepository.findByIdWithAuthor(postId);
        PostResponse postResponse = modelMapper.map(updatedPost, PostResponse.class);
        return ResponseEntity.ok(WrapRes.success(Map.of(
                "message", "Post status updated successfully",
                "post", postResponse)));
    }

    // ==================== GROUP MANAGEMENT APIs ====================

    @Operation(summary = "Lấy danh sách tất cả group", description = "Chỉ ADMIN hoặc OWNER mới có quyền truy cập.")
    @ApiResponse(responseCode = "200", description = "Danh sách group")
    @GetMapping("/groups")
    public ResponseEntity<WrapRes<List<GroupResponse>>> getAllGroups() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        List<GroupResponse> groups = groupService.getAllGroupsForAdmin();
        return ResponseEntity.ok(WrapRes.success(groups));
    }

    @Operation(summary = "Cập nhật trạng thái group", description = "Chỉ ADMIN hoặc OWNER mới có quyền cập nhật trạng thái group.")
    @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái group thành công")
    @PatchMapping("/groups/{groupId}/status")
    public ResponseEntity<WrapRes<?>> updateGroupStatus(
            @Parameter(description = "ID của group cần cập nhật trạng thái") @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Trạng thái mới") @RequestBody AdminGroupStatusRequest statusRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        GroupResponse updatedGroup = groupService.updateGroupStatus(groupId, statusRequest.getStatus(), statusRequest.getAdminNotes(), currentUser.getId());
        return ResponseEntity.ok(WrapRes.success(Map.of(
                "message", "Group status updated successfully",
                "group", updatedGroup)));
    }

    @Operation(summary = "Xóa group", description = "Chỉ ADMIN hoặc OWNER mới có quyền xóa group.")
    @ApiResponse(responseCode = "200", description = "Xóa group thành công")
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<WrapRes<?>> deleteGroup(
            @Parameter(description = "ID của group cần xóa") @PathVariable Long groupId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        groupService.adminDeleteGroup(groupId);
        return ResponseEntity.ok(WrapRes.success(Map.of("message", "Group deleted successfully")));
    }

    @Operation(summary = "Lấy thống kê group", description = "Chỉ ADMIN hoặc OWNER mới có quyền xem thống kê.")
    @ApiResponse(responseCode = "200", description = "Thống kê group")
    @GetMapping("/groups/{groupId}/analytics")
    public ResponseEntity<WrapRes<GroupAnalyticsResponse>> getGroupAnalytics(
            @Parameter(description = "ID của group") @PathVariable Long groupId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        GroupAnalyticsResponse analytics = groupService.getGroupAnalytics(groupId);
        return ResponseEntity.ok(WrapRes.success(analytics));
    }

    // ==================== REPORT MANAGEMENT APIs ====================

    @Operation(summary = "Lấy danh sách tất cả report", description = "Chỉ ADMIN hoặc OWNER mới có quyền truy cập.")
    @ApiResponse(responseCode = "200", description = "Danh sách report")
    @GetMapping("/reports")
    public ResponseEntity<WrapRes<List<ReportResponse>>> getAllReports() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        List<ReportResponse> reports = reportService.getAllReports();
        return ResponseEntity.ok(WrapRes.success(reports));
    }

    @Operation(summary = "Xem chi tiết report", description = "Chỉ ADMIN hoặc OWNER mới có quyền xem chi tiết report.")
    @ApiResponse(responseCode = "200", description = "Chi tiết report")
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<WrapRes<ReportResponse>> getReportById(
            @Parameter(description = "ID của report") @PathVariable Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        ReportResponse report = reportService.getReportById(reportId);
        return ResponseEntity.ok(WrapRes.success(report));
    }

    @Operation(summary = "Xử lý report", description = "Chỉ ADMIN hoặc OWNER mới có quyền xử lý report.")
    @ApiResponse(responseCode = "200", description = "Xử lý report thành công")
    @PatchMapping("/reports/{reportId}/review")
    public ResponseEntity<WrapRes<?>> reviewReport(
            @Parameter(description = "ID của report cần xử lý") @PathVariable Long reportId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Thông tin xử lý") @RequestBody AdminReportReviewRequest reviewRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        ReportResponse updatedReport = reportService.reviewReport(reportId, reviewRequest.getStatus(), 
                reviewRequest.getAdminNotes(), reviewRequest.getPriority(), currentUser.getId());
        return ResponseEntity.ok(WrapRes.success(Map.of(
                "message", "Report reviewed successfully",
                "report", updatedReport)));
    }

    @Operation(summary = "Lấy danh sách report theo trạng thái", description = "Chỉ ADMIN hoặc OWNER mới có quyền truy cập.")
    @ApiResponse(responseCode = "200", description = "Danh sách report theo trạng thái")
    @GetMapping("/reports/status/{status}")
    public ResponseEntity<WrapRes<List<ReportResponse>>> getReportsByStatus(
            @Parameter(description = "Trạng thái report") @PathVariable String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        List<ReportResponse> reports = reportService.getReportByStatus(status);
        return ResponseEntity.ok(WrapRes.success(reports));
    }

    @Operation(summary = "Lấy thống kê report", description = "Chỉ ADMIN hoặc OWNER mới có quyền xem thống kê.")
    @ApiResponse(responseCode = "200", description = "Thống kê report")
    @GetMapping("/reports/analytics")
    public ResponseEntity<WrapRes<ReportAnalyticsResponse>> getReportAnalytics() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        ReportAnalyticsResponse analytics = reportService.getReportAnalytics();
        return ResponseEntity.ok(WrapRes.success(analytics));
    }

    @Operation(summary = "Xóa report", description = "Chỉ ADMIN hoặc OWNER mới có quyền xóa report.")
    @ApiResponse(responseCode = "200", description = "Xóa report thành công")
    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<WrapRes<?>> deleteReport(
            @Parameter(description = "ID của report cần xóa") @PathVariable Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);

        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required."));
        }

        reportService.deleteReport(reportId);
        return ResponseEntity.ok(WrapRes.success(Map.of("message", "Report deleted successfully")));
    }
}