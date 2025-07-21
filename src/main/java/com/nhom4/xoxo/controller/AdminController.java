package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdminController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
        }
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
        }
        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete your own account"));
        }
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId, @RequestBody Map<String, Boolean> statusRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
        }
        Boolean enabled = statusRequest.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "enabled field is required"));
        }
        User updatedUser = userService.toggleUserStatus(userId, enabled);
        return ResponseEntity.ok(Map.of(
            "message", "User status updated successfully",
            "user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "enabled", updatedUser.isEnabled(),
                "updatedAt", updatedUser.getUpdatedAt()
            )
        ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        User targetUser = userService.findById(userId);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }
        if (!currentUser.getId().equals(userId) && !currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        return ResponseEntity.ok(Map.of(
            "id", targetUser.getId(),
            "email", targetUser.getEmail(),
            "firstName", targetUser.getFirstName(),
            "lastName", targetUser.getLastName(),
            "enabled", targetUser.isEnabled(),
            "authProvider", targetUser.getAuthProvider(),
            "roles", targetUser.getRoles(),
            "createdAt", targetUser.getCreatedAt(),
            "updatedAt", targetUser.getUpdatedAt()
        ));
    }
} 