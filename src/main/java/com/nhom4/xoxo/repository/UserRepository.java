package com.nhom4.xoxo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.AuthProvider;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByAuthProvider(AuthProvider authProvider);
}
