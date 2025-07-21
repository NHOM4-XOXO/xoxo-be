package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = "*", maxAge = 3600)
public class OwnerController {
    @Autowired
    private UserService userService;

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<?> addRoleToUser(@PathVariable Long userId, @RequestBody Map<String, String> roleRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
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
        return ResponseEntity.ok(Map.of(
            "message", "Role added successfully",
            "user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            )
        ));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long userId, @PathVariable String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!currentUser.getRoles().stream().anyMatch(role -> role.equals(Role.OWNER))) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Owner role required."));
        }
        Role role;
        try {
            role = Role.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + roleName));
        }
        if (role.equals(Role.OWNER)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot remove OWNER role"));
        }
        User updatedUser = userService.removeRoleFromUser(userId, role);
        return ResponseEntity.ok(Map.of(
            "message", "Role removed successfully",
            "user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            )
        ));
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<?> setUserRoles(@PathVariable Long userId, @RequestBody Map<String, Object> rolesRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
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
        return ResponseEntity.ok(Map.of(
            "message", "Roles set successfully",
            "user", Map.of(
                "id", updatedUser.getId(),
                "email", updatedUser.getEmail(),
                "roles", updatedUser.getRoles(),
                "updatedAt", updatedUser.getUpdatedAt()
            )
        ));
    }

    @PostMapping("/users/admin")
    public ResponseEntity<?> createAdminUser(@RequestBody Map<String, String> adminRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
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
        return ResponseEntity.ok(Map.of(
            "message", "Admin user created successfully",
            "user", Map.of(
                "id", newAdmin.getId(),
                "email", newAdmin.getEmail(),
                "firstName", newAdmin.getFirstName(),
                "lastName", newAdmin.getLastName(),
                "roles", newAdmin.getRoles(),
                "enabled", newAdmin.isEnabled(),
                "createdAt", newAdmin.getCreatedAt()
            )
        ));
    }
} 