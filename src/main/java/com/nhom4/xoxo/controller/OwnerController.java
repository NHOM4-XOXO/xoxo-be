package com.nhom4.xoxo.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.RoleRequest;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final UserService userService;

    private final ModelMapper modelMapper;

    public OwnerController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    @Operation(
        summary = "Thêm role cho user",
        description = "Chỉ OWNER mới có quyền thêm role cho user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Thêm role thành công")
        }
    )
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<WrapRes<?>> addRoleToUser(@PathVariable Long userId, @RequestBody @Valid RoleRequest roleRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!userService.canAddRole(currentUser)) {
            return ResponseEntity.status(403).body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Owner role required."));
        }
        Role role = Role.valueOf(roleRequest.getRole().toString().toUpperCase());
        User updatedUser = userService.addRoleToUser(userId, role, currentUser);
        return ResponseEntity.ok(WrapRes.success("Role added successfully; New roles: " + updatedUser.getRoles().toString()));
    }

    @Operation(
        summary = "Xóa role khỏi user",
        description = "Chỉ OWNER mới có quyền xóa role khỏi user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Xóa role thành công")
        }
    )
    @DeleteMapping("/users/{userId}/roles")
    public ResponseEntity<WrapRes<?>> removeRoleFromUser(@PathVariable Long userId, @RequestBody @Valid RoleRequest roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        Role role = Role.valueOf(roleName.getRole().toString().toUpperCase());
        if (!userService.canRemoveRole(currentUser, role)) {
            return ResponseEntity.status(403).body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied or cannot remove OWNER role."));
        }
        User updatedUser = userService.removeRoleFromUser(userId, role, currentUser);
        return ResponseEntity.ok(WrapRes.success("Role removed successfully; New roles: " + updatedUser.getRoles().toString()));
    }

    @Operation(
        summary = "Set lại toàn bộ role cho user",
        description = "Chỉ OWNER mới có quyền set lại toàn bộ role cho user.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Set role thành công")
        }
    )
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<WrapRes<?>> setUserRoles(@PathVariable Long userId, @RequestBody @Valid List<RoleRequest> rolesRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!userService.canSetUserRoles(currentUser)) {
            return ResponseEntity.status(403).body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Owner role required."));
        }
        Set<Role> currentRoles = currentUser.getRoles();
        Set<Role> roleReq = rolesRequest.stream().map(RoleRequest::getRole).collect(Collectors.toSet());
        Set<Role> newRoles = new HashSet<>(currentRoles);
        newRoles.addAll(roleReq);

        User updatedUser = userService.setUserRoles(userId, newRoles, currentUser);
        return ResponseEntity.ok(WrapRes.success("Roles set successfully; New roles: " + updatedUser.getRoles().toString()));
    }

    @Operation(
        summary = "Tạo tài khoản admin mới",
        description = "Chỉ OWNER mới có quyền tạo tài khoản admin.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tạo admin thành công")
        }
    )
    @PostMapping("/users/admin")
    public ResponseEntity<WrapRes<?>> createAdminUser(@RequestBody @Valid RegisterRequest adminRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userService.findByEmail(currentUserEmail);
        if (!userService.canCreateAdminUser(currentUser)) {
            return ResponseEntity.status(403).body(WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied. Owner role required."));
        }
        String email = adminRequest.getEmail();
        String password = adminRequest.getPassword();
        String firstName = adminRequest.getFirstName();
        String lastName = adminRequest.getLastName();

        User newAdmin = userService.createAdminUser(email, password, firstName, lastName);
        UserResponse userResponse = modelMapper.map(newAdmin, UserResponse.class);

        return ResponseEntity.ok(WrapRes.success(userResponse));
    }
}