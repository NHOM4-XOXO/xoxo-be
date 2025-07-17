package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Lấy thông tin user hiện tại (đã đăng nhập)
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("enabled", user.isEnabled());
            response.put("authProvider", user.getAuthProvider());
            response.put("roles", user.getRoles());
            response.put("createdAt", user.getCreatedAt());
            response.put("updatedAt", user.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get user profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Cập nhật thông tin user
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, String> updateRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            // Cập nhật thông tin
            if (updateRequest.containsKey("firstName")) {
                user.setFirstName(updateRequest.get("firstName"));
            }
            if (updateRequest.containsKey("lastName")) {
                user.setLastName(updateRequest.get("lastName"));
            }

            User updatedUser = userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "firstName", updatedUser.getFirstName(),
                "lastName", updatedUser.getLastName(),
                "enabled", updatedUser.isEnabled(),
                "authProvider", updatedUser.getAuthProvider(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy thông tin user theo ID (chỉ admin hoặc chính user đó)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            User targetUser = userService.findById(userId);
            
            if (targetUser == null) {
                return ResponseEntity.notFound().build();
            }

            // Kiểm tra quyền: chỉ user đó hoặc admin/owner mới được xem
            if (!currentUser.getId().equals(userId) && 
                !currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", targetUser.getId());
            response.put("email", targetUser.getEmail());
            response.put("firstName", targetUser.getFirstName());
            response.put("lastName", targetUser.getLastName());
            response.put("enabled", targetUser.isEnabled());
            response.put("authProvider", targetUser.getAuthProvider());
            response.put("roles", targetUser.getRoles());
            response.put("createdAt", targetUser.getCreatedAt());
            response.put("updatedAt", targetUser.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy danh sách tất cả users (chỉ admin/owner)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Kiểm tra quyền admin hoặc owner
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
            }

            return ResponseEntity.ok(userService.findAllUsers());
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get users: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Xóa user (chỉ admin/owner)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Kiểm tra quyền admin hoặc owner
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
            }

            // Không cho phép xóa chính mình
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete your own account"));
            }

            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Kích hoạt/vô hiệu hóa user (chỉ admin/owner)
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId, @RequestBody Map<String, Boolean> statusRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Kiểm tra quyền admin hoặc owner
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.ADMIN) || role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Admin or Owner role required."));
            }

            Boolean enabled = statusRequest.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "enabled field is required"));
            }

            User updatedUser = userService.toggleUserStatus(userId, enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User status updated successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "enabled", updatedUser.isEnabled(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update user status: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Thêm role cho user (chỉ owner)
     */
    @PostMapping("/{userId}/roles")
    public ResponseEntity<?> addRoleToUser(@PathVariable Long userId, @RequestBody Map<String, String> roleRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Chỉ owner mới được thêm role
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Owner role required."));
            }

            String roleName = roleRequest.get("role");
            if (roleName == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "role field is required"));
            }

            Role role;
            try {
                role = Role.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleName));
            }

            User updatedUser = userService.addRoleToUser(userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role added successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to add role: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Xóa role khỏi user (chỉ owner)
     */
    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long userId, @PathVariable String roleName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Chỉ owner mới được xóa role
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Owner role required."));
            }

            Role role;
            try {
                role = Role.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleName));
            }

            // Không cho phép xóa role OWNER
            if (role.equals(Role.OWNER)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot remove OWNER role"));
            }

            User updatedUser = userService.removeRoleFromUser(userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role removed successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to remove role: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Set role cho user (chỉ owner)
     */
    @PutMapping("/{userId}/roles")
    public ResponseEntity<?> setUserRoles(@PathVariable Long userId, @RequestBody Map<String, Object> rolesRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Chỉ owner mới được set role
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Owner role required."));
            }

            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) rolesRequest.get("roles");
            if (roleNames == null || roleNames.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "roles field is required and cannot be empty"));
            }

            Set<Role> roles = new HashSet<>();
            for (String roleName : roleNames) {
                try {
                    Role role = Role.valueOf(roleName.toUpperCase());
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleName));
                }
            }

            User updatedUser = userService.setUserRoles(userId, roles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Roles set successfully");
            response.put("user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to set roles: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Tạo user với role admin (chỉ owner)
     */
    @PostMapping("/admin")
    public ResponseEntity<?> createAdminUser(@RequestBody Map<String, String> adminRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            User currentUser = userService.findByEmail(currentUserEmail);
            
            // Chỉ owner mới được tạo admin
            if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER))) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Owner role required."));
            }

            String email = adminRequest.get("email");
            String password = adminRequest.get("password");
            String firstName = adminRequest.get("firstName");
            String lastName = adminRequest.get("lastName");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "email and password are required"));
            }

            User newAdmin = userService.createAdminUser(email, password, firstName, lastName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Admin user created successfully");
            response.put("user", Map.of(
                "id", newAdmin.getId(),
                "email", newAdmin.getEmail(),
                "firstName", newAdmin.getFirstName(),
                "lastName", newAdmin.getLastName(),
                "roles", newAdmin.getRoles(),
                "enabled", newAdmin.isEnabled(),
                "createdAt", newAdmin.getCreatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create admin user: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 