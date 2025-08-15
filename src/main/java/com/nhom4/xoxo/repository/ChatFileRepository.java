package com.nhom4.xoxo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom4.xoxo.entity.ChatFile;

public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {
}
