package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public WrapRes<List<UserResponseProjection>> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required.");
        }
        return WrapRes.success(userService.findAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    public WrapRes<?> deleteUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            throw new ForbiddenException("Access denied. Admin or Owner role required.");
        }
        if (currentUser.getId().equals(userId)) {
            throw new ServiceException("Cannot delete your own account");

        }
        userService.deleteUser(userId, currentUser);
        return WrapRes.success(Map.of("message", "User deleted successfully"));
    }

    @PatchMapping("/users/{userId}/status")
    public WrapRes<?> toggleUserStatus(@PathVariable Long userId, @RequestBody Map<String, Boolean> statusRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Admin or Owner role required.");
        }
        Boolean enabled = statusRequest.get("enabled");
        if (enabled == null) {
            return WrapRes.error(WrapResStatus.BAD_REQUEST, "enabled field is required");
        }
        User updatedUser = userService.toggleUserStatus(userId, enabled, currentUser);
        return WrapRes.success(Map.of(
                "message", "User status updated successfully",
                "user", Map.of(
                        "id", updatedUser.getId(),
                        "email", updatedUser.getEmail(),
                        "enabled", updatedUser.isEnabled(),
                        "updatedAt", updatedUser.getUpdatedAt())));
    }

    @GetMapping("/users/{userId}")
    public WrapRes<?> getUserById(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        User targetUser = userService.findById(userId, currentUser);
        if (targetUser == null) {
           throw new NotFoundException("User not found");
        }
        if (!currentUser.getId().equals(userId) && !currentUser.getRoles().stream()
                .anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            throw new ForbiddenException("Access denied");
        }
        return WrapRes.success(Map.of(
                "id", targetUser.getId(),
                "email", targetUser.getEmail(),
                "firstName", targetUser.getFirstName(),
                "lastName", targetUser.getLastName(),
                "enabled", targetUser.isEnabled(),
                "authProvider", targetUser.getAuthProvider(),
                "roles", targetUser.getRoles(),
                "createdAt", targetUser.getCreatedAt(),
                "updatedAt", targetUser.getUpdatedAt()));
    }
}