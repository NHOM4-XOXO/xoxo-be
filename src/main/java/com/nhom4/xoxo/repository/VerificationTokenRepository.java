package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.VerificationToken;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    Optional<VerificationToken> findByTokenAndType(String token, String type);
    
    List<VerificationToken> findByUserAndType(User user, String type);
} 