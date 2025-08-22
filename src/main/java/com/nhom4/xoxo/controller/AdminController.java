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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.ToggleUserStatusRequest;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
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
}