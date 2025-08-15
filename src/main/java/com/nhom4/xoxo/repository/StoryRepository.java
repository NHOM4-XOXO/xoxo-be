package com.nhom4.xoxo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Story;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.PrivacyLevel;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    // Tìm story theo ID
    Optional<Story> findById(Long storyId);

    // Tìm stories theo user
    List<Story> findByUser(User user);

    // Tìm stories theo user và privacy
    List<Story> findByUserAndPrivacy(User user, PrivacyLevel privacy);

    // Tìm stories public
    List<Story> findByPrivacy(PrivacyLevel privacy);

    // Tìm stories public với pagination
    Page<Story> findByPrivacyOrderByCreatedAtDesc(PrivacyLevel privacy, Pageable pageable);

    // Tìm stories của user với pagination
    Page<Story> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Tìm stories theo content
    @Query("SELECT s FROM Story s WHERE s.content LIKE %:content%")
    List<Story> findByContentContaining(@Param("content") String content);

    // Tìm stories trong khoảng thời gian
    @Query("SELECT s FROM Story s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<Story> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Đếm stories theo user
    @Query("SELECT COUNT(s) FROM Story s WHERE s.user = :user")
    Long countByUser(@Param("user") User user);

    // Đếm stories public theo user
    @Query("SELECT COUNT(s) FROM Story s WHERE s.user = :user AND s.privacy = :privacy")
    Long countByUserAndPrivacy(@Param("user") User user, @Param("privacy") PrivacyLevel privacy);

    // Tìm stories của friends (cho newsfeed)
    @Query("SELECT s FROM Story s WHERE s.user.id IN :friendIds AND s.privacy IN :privacyLevels ORDER BY s.createdAt DESC")
    List<Story> findStoriesForNewsfeed(@Param("friendIds") List<Long> friendIds,
            @Param("privacyLevels") List<PrivacyLevel> privacyLevels);

    // Xóa stories cũ hơn 24 giờ (stories thường tự động xóa sau 24h)
    @Query("DELETE FROM Story s WHERE s.createdAt < :cutoffTime")
    void deleteStoriesOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT m FROM MediaRoom mr JOIN mr.media m JOIN FETCH m.uploadedBy WHERE mr.targetId = :storyId AND mr.targetType = :targetType")
    List<Media> findStoryMediaWithUploadedBy(@Param("storyId") Long storyId,
            @Param("targetType") MediaRoomTargetType targetType);
}