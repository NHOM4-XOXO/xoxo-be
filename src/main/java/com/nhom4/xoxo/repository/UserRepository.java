package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
