package com.nhom4.xoxo.service;

import java.util.List;
import java.util.Set;

import com.nhom4.xoxo.dto.req.ForgotPasswordRequest;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.ResetPasswordRequest;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;

public interface UserService {
    User registerUser(RegisterRequest request);
    User findByEmail(String email);
    User findById(Long id, User currentUser);
    User updateUser(User user, User currentUser);
    User toggleUserStatus(Long userId, boolean enabled, User currentUser);
    void deleteUser(Long userId, User currentUser);
    List<UserResponseProjection> findAllUsersAdmin();
    List<UserResponseProjection> findAllUsersOwner();
    User addRoleToUser(Long userId, Role role, User currentUser);
    User removeRoleFromUser(Long userId, Role role, User currentUser);
    User setUserRoles(Long userId, Set<Role> roles, User currentUser);
    User createAdminUser(String email, String password, String firstName, String lastName);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
} 