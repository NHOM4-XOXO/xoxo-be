package com.nhom4.xoxo.service;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.dto.req.RegisterRequest;
import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.Role;
import java.util.List;
import java.util.Set;

public interface UserService {
    User registerUser(RegisterRequest request);
    User findByEmail(String email);
    User findById(Long id, User currentUser);
    User updateUser(User user, User currentUser);
    User toggleUserStatus(Long userId, boolean enabled, User currentUser);
    void deleteUser(Long userId, User currentUser);
    List<UserResponseProjection> findAllUsers();
    User addRoleToUser(Long userId, Role role);
    User removeRoleFromUser(Long userId, Role role);
    User setUserRoles(Long userId, Set<Role> roles);
    User createAdminUser(String email, String password, String firstName, String lastName);
} 