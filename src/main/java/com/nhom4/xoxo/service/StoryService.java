package com.nhom4.xoxo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Story;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PrivacyLevel;

public interface StoryService {

    // Tạo story mới
    Story createStory(Story story);

    // Lấy story theo ID
    Optional<Story> getStoryById(Long storyId);

    // Lấy tất cả stories public
    List<Story> getPublicStories();

    // Lấy stories theo user
    List<Story> getStoriesByUser(User user);

    // Lấy stories theo user với pagination
    Page<Story> getStoriesByUser(User user, Pageable pageable);

    // Lấy stories public với pagination
    Page<Story> getPublicStories(Pageable pageable);

    // Lấy stories theo privacy level
    List<Story> getStoriesByPrivacy(PrivacyLevel privacy);

    // Lấy media của story
    List<Media> getStoryMedia(Long storyId);

    // Thêm media cho story
    void addMediaToStory(Long storyId, Long mediaId);

    // Xóa media khỏi story
    void removeMediaFromStory(Long storyId, Long mediaId);

    // Cập nhật story
    Story updateStory(Long storyId, Story updatedStory);

    // Xóa story
    void deleteStory(Long storyId);

    // Tìm stories theo content
    List<Story> searchStoriesByContent(String content);

    // Tìm stories trong khoảng thời gian
    List<Story> getStoriesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // Đếm stories theo user
    Long countStoriesByUser(User user);

    // Lấy stories cho newsfeed (của friends)
    List<Story> getStoriesForNewsfeed(List<Long> friendIds, List<PrivacyLevel> privacyLevels);

    // Xóa stories cũ (tự động sau 24h)
    void deleteExpiredStories();

    // Kiểm tra quyền xem story
    boolean canViewStory(User currentUser, Story story);

    // Kiểm tra quyền chỉnh sửa story
    boolean canEditStory(User currentUser, Story story);

    // Kiểm tra quyền xóa story
    boolean canDeleteStory(User currentUser, Story story);
}