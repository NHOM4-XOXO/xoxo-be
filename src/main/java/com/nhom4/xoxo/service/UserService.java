package com.nhom4.xoxo.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;

import com.nhom4.xoxo.dto.req.ForgotPasswordRequest;
import com.nhom4.xoxo.dto.req.LoginRequest;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.req.ResetPasswordRequest;
import com.nhom4.xoxo.dto.res.LoginResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;

import jakarta.servlet.http.HttpServletResponse;

public interface UserService {
    UserResponse registerUser(RegisterRequest request);
    User findByEmail(String email);
    User findById(Long id, User currentUser);
    User findById(Long id);
    User updateUser(User user, User currentUser);
    User toggleUserStatus(Long userId, boolean enabled, User currentUser);
    void deleteUser(Long userId, User currentUser);
    List<UserResponseProjection> findAllUsersAdmin();
    List<UserResponseProjection> findAllUsersOwner();
    User addRoleToUser(Long userId, Role role, User currentUser);
    User removeRoleFromUser(Long userId, Role role, User currentUser);
    User setUserRoles(Long userId, Set<Role> roles, User currentUser);
    User createAdminUser(String email, String password, String firstName, String lastName);
   
    void resetPassword(ResetPasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void regenerateForgotPassword(ForgotPasswordRequest request);
    LoginResponse login(LoginRequest request, HttpServletResponse response);
    String refreshToken(String refreshToken);
    String verifyAccount(String token);
    boolean logout(String refreshToken, HttpServletResponse response);
    boolean changePassword(String oldPassword, String newPassword, UserDetails currentUser);
    // Kiểm tra quyền
    boolean isAdminOrOwner(User user);
    boolean isSelf(User currentUser, Long userId);
    boolean canViewUser(User currentUser, Long targetUserId);
    boolean canDeleteUser(User currentUser, Long targetUserId);
    boolean canToggleUserStatus(User currentUser);
    // Kiểm tra quyền cho OwnerController
    boolean isOwner(User user);
    boolean canAddRole(User currentUser);
    boolean canRemoveRole(User currentUser, Role role);
    boolean canSetUserRoles(User currentUser);
    boolean canCreateAdminUser(User currentUser);
    boolean canUpdateUsername(User currentUser);
    boolean updateUsername(User currentUser, String username);
    boolean updateAvatar(User currentUser, String avatar);
    boolean updateCover(User currentUser, String cover);
    Optional<User> findByUsername(String username);
    void resendVerificationEmail(String email);
} 