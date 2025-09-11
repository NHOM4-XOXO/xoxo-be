package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.dto.res.UserResponseProjection;
import com.nhom4.xoxo.entity.AuthProvider;
import com.nhom4.xoxo.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByAuthProvider(AuthProvider authProvider);

    @Query(value = "SELECT id, email, first_name AS firstName, last_name AS lastName, roles, date_of_birth AS dateOfBirth, gender, avatar_url AS avatarUrl, cover_url AS coverUrl, bio, created_at AS createdAt, updated_at AS updatedAt, enabled FROM view_users_exclude_owner_admin", nativeQuery = true)
    List<UserResponseProjection> findAllUserResponsesAdmin();

    @Query(value = "SELECT id, email, first_name AS firstName, last_name AS lastName, roles, date_of_birth AS dateOfBirth, gender, avatar_url AS avatarUrl, cover_url AS coverUrl, bio, created_at AS createdAt, updated_at AS updatedAt, enabled FROM view_users_exclude_owner", nativeQuery = true)
    List<UserResponseProjection> findAllUserResponsesOwner();

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
    
    // Search methods
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.bio) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.bio) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsersPageable(@Param("keyword") String keyword, Pageable pageable);
}
