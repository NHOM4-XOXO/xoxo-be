package com.nhom4.xoxo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom4.xoxo.entity.UserEncryptionKey;

public interface UserEncryptionKeyRepository extends JpaRepository<UserEncryptionKey, Long> {
	Optional<UserEncryptionKey> findByUserId(Long userId);
}
