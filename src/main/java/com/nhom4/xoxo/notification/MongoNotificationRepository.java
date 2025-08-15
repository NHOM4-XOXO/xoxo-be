package com.nhom4.xoxo.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoNotificationRepository extends MongoRepository<MongoNotification, String> {
    
    @Query("{'userId': ?0}")
    Page<MongoNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("{'userId': ?0, 'read': false}")
    List<MongoNotification> findUnreadByUserId(Long userId);
    
    @Query("{'userId': ?0, 'read': false}")
    long countUnreadByUserId(Long userId);
    
    @Query("{'userId': ?0, 'type': ?1}")
    List<MongoNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
}