package com.nhom4.xoxo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.Story;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.PrivacyLevel;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.MediaRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;
import com.nhom4.xoxo.repository.StoryRepository;
import com.nhom4.xoxo.service.FriendshipService;
import com.nhom4.xoxo.service.StoryService;

@Service
@Transactional("transactionManager")
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final MediaRoomRepository mediaRoomRepository;
    private final MediaRepository mediaRepository;
    private final FriendshipService friendshipService;

    public StoryServiceImpl(StoryRepository storyRepository, MediaRoomRepository mediaRoomRepository,
            MediaRepository mediaRepository, FriendshipService friendshipService) {
        this.storyRepository = storyRepository;
        this.mediaRoomRepository = mediaRoomRepository;
        this.mediaRepository = mediaRepository;
        this.friendshipService = friendshipService;
    }

    @Override
    public Story createStory(Story story) {
        return storyRepository.save(story);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public Optional<Story> getStoryById(Long storyId) {
        Optional<Story> story = storyRepository.findById(storyId);
        if (story.isEmpty()) {
            throw new NotFoundException("Story not found");
        }
        return story;
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> getPublicStories() {
        return storyRepository.findByPrivacy(PrivacyLevel.PUBLIC);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> getStoriesByUser(User user) {
        return storyRepository.findByUser(user);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Story> getStoriesByUser(User user, Pageable pageable) {
        return storyRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public Page<Story> getPublicStories(Pageable pageable) {
        return storyRepository.findByPrivacyOrderByCreatedAtDesc(PrivacyLevel.PUBLIC, pageable);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> getStoriesByPrivacy(PrivacyLevel privacy) {
        return storyRepository.findByPrivacy(privacy);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Media> getStoryMedia(Long storyId) {
        return storyRepository.findStoryMediaWithUploadedBy(storyId, MediaRoomTargetType.STORY);
    }

    @Override
    public void addMediaToStory(Long storyId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found with id: " + mediaId));

        MediaRoom mediaRoom = MediaRoom.builder()
                .media(media)
                .targetId(storyId)
                .targetType(MediaRoomTargetType.STORY)
                .build();

        mediaRoomRepository.save(mediaRoom);
    }

    @Override
    public void removeMediaFromStory(Long storyId, Long mediaId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
                storyId, MediaRoomTargetType.STORY);

        mediaRooms.stream()
                .filter(mr -> mr.getMedia().getId().equals(mediaId))
                .findFirst()
                .ifPresent(mediaRoomRepository::delete);
    }

    @Override
    public Story updateStory(Long storyId, Story updatedStory) {
        Story existingStory = getStoryById(storyId).get();

        existingStory.setContent(updatedStory.getContent());
        existingStory.setPrivacy(updatedStory.getPrivacy());
        existingStory.setUpdatedAt(LocalDateTime.now());

        return storyRepository.save(existingStory);
    }

    @Override
    public void deleteStory(Long storyId) {
        Story story = getStoryById(storyId).get();
        storyRepository.delete(story);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> searchStoriesByContent(String content) {
        return storyRepository.findByContentContaining(content);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> getStoriesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return storyRepository.findByCreatedAtBetween(startDate, endDate);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public Long countStoriesByUser(User user) {
        return storyRepository.countByUser(user);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<Story> getStoriesForNewsfeed(List<Long> friendIds, List<PrivacyLevel> privacyLevels) {
        return storyRepository.findStoriesForNewsfeed(friendIds, privacyLevels);
    }

    @Override
    public void deleteExpiredStories() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        storyRepository.deleteStoriesOlderThan(cutoffTime);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public boolean canViewStory(User currentUser, Story story) {
        // Admin và Owner có thể xem tất cả
        if (currentUser.getRoles().contains(Role.ADMIN) || currentUser.getRoles().contains(Role.OWNER)) {
            return true;
        }

        // Chủ sở hữu có thể xem story của mình
        if (story.getUser().getId().equals(currentUser.getId())) {
            return true;
        }

        // Story public thì ai cũng xem được
        if (story.getPrivacy() == PrivacyLevel.PUBLIC) {
            return true;
        }

        // Story friends thì cần kiểm tra friendship
        if (story.getPrivacy() == PrivacyLevel.FRIENDS) {
            // Check if current user is friend with story owner
            try {
                // We need FriendshipService to check this
                // For now, implement basic logic
                return friendshipService.areFriends(currentUser.getId(), story.getUser().getId()).isAreFriends();
            } catch (Exception e) {
                // If error, deny access for security
                return false;
            }
        }

        // Story private chỉ chủ sở hữu mới xem được
        return false;
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public boolean canEditStory(User currentUser, Story story) {
        // Chỉ chủ sở hữu mới có thể chỉnh sửa
        return story.getUser().getId().equals(currentUser.getId());
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public boolean canDeleteStory(User currentUser, Story story) {
        // Admin, Owner và chủ sở hữu có thể xóa
        if (currentUser.getRoles().contains(Role.ADMIN) || currentUser.getRoles().contains(Role.OWNER)) {
            return true;
        }

        return story.getUser().getId().equals(currentUser.getId());
    }
}