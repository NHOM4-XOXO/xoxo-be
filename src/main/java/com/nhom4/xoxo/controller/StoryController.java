package com.nhom4.xoxo.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.StoryRequest;
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.dto.res.StoryResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Story;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PrivacyLevel;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.service.StoryService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/stories")
@Slf4j
public class StoryController {

    private final StoryService storyService;
    private final UserService userService;

    public StoryController(StoryService storyService, UserService userService) {
        this.storyService = storyService;
        this.userService = userService;
    }

    @Operation(summary = "Tạo story mới", description = "Yêu cầu đã đăng nhập. Tạo story mới.", responses = {
            @ApiResponse(responseCode = "200", description = "Tạo story thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping
    public ResponseEntity<WrapRes<?>> createStory(@RequestBody @Valid StoryRequest storyRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);

        // Map StoryRequest to Story entity
        Story story = Story.builder()
                .content(storyRequest.getContent())
                .privacy(storyRequest.getPrivacy())
                .user(user)
                .build();

        Story createdStory = storyService.createStory(story);

        // Add media if provided
        if (storyRequest.getMediaIds() != null && !storyRequest.getMediaIds().isEmpty()) {
            for (Long mediaId : storyRequest.getMediaIds()) {
                storyService.addMediaToStory(createdStory.getId(), mediaId);
            }
        }

        StoryResponse storyResponse = mapToStoryResponse(createdStory);

        return ResponseEntity.ok(WrapRes.success(storyResponse));
    }

    @Operation(summary = "Lấy story theo ID", description = "Lấy thông tin chi tiết story theo ID", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy story thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy story")
    })
    @GetMapping("/{storyId}")
    public ResponseEntity<WrapRes<?>> getStoryById(@PathVariable Long storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Story story = storyService.getStoryById(storyId).get();

        // Kiểm tra quyền xem story
        if (!storyService.canViewStory(currentUser, story)) {
            throw new ForbiddenException("You don't have permission to view this story");
        }

        StoryResponse storyResponse = mapToStoryResponse(story);
        return ResponseEntity.ok(WrapRes.success(storyResponse));
    }

    @Operation(summary = "Lấy tất cả stories public", description = "Lấy danh sách tất cả stories public với pagination", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách stories thành công")
    })
    @GetMapping("/public")
    public ResponseEntity<WrapRes<?>> getPublicStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyService.getPublicStories(pageable);
        Page<StoryResponse> storyResponses = stories.map(this::mapToStoryResponse);
        log.info("Found {} public stories", storyResponses.getTotalElements());
        return ResponseEntity.ok(WrapRes.success(storyResponses));
    }

    @Operation(summary = "Lấy stories theo user", description = "Lấy danh sách stories của một user", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách stories thành công")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<WrapRes<?>> getStoriesByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);
        User targetUser = userService.findById(userId, currentUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyService.getStoriesByUser(targetUser, pageable);

        // Filter stories based on privacy and current user permissions
        Page<StoryResponse> storyResponses = stories
                .map(story -> storyService.canViewStory(currentUser, story) ? mapToStoryResponse(story) : null)
                .map(response -> response); // Remove null values would need additional filtering

        return ResponseEntity.ok(WrapRes.success(storyResponses));
    }

    @Operation(summary = "Thêm media cho story", description = "Thêm một hoặc nhiều media vào story", responses = {
            @ApiResponse(responseCode = "200", description = "Thêm media thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy story hoặc media")
    })
    @PostMapping("/{storyId}/media")
    public ResponseEntity<WrapRes<?>> addMediaToStory(
            @PathVariable Long storyId,
            @RequestParam("mediaIds") List<Long> mediaIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Story story = storyService.getStoryById(storyId).get();

        // Kiểm tra quyền chỉnh sửa story
        if (!storyService.canEditStory(currentUser, story)) {
            throw new ForbiddenException("You don't have permission to edit this story");
        }

        for (Long mediaId : mediaIds) {
            storyService.addMediaToStory(storyId, mediaId);
        }
        return ResponseEntity.ok(WrapRes.success("Media added to story successfully"));
    }

    @Operation(summary = "Xóa media khỏi story", description = "Xóa media khỏi story", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa media thành công")
    })
    @DeleteMapping("/{storyId}/media/{mediaId}")
    public ResponseEntity<WrapRes<?>> removeMediaFromStory(@PathVariable Long storyId, @PathVariable Long mediaId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Story story = storyService.getStoryById(storyId).get();

        // Kiểm tra quyền chỉnh sửa story
        if (!storyService.canEditStory(currentUser, story)) {
            throw new ForbiddenException("You don't have permission to edit this story");
        }

        storyService.removeMediaFromStory(storyId, mediaId);
        return ResponseEntity.ok(WrapRes.success("Media removed from story successfully"));
    }

    @Operation(summary = "Cập nhật story", description = "Cập nhật thông tin story", responses = {
            @ApiResponse(responseCode = "200", description = "Cập nhật story thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy story")
    })
    @PutMapping("/{storyId}")
    public ResponseEntity<WrapRes<?>> updateStory(@PathVariable Long storyId,
            @RequestBody @Valid StoryRequest storyRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Story existingStory = storyService.getStoryById(storyId).get();

        // Kiểm tra quyền chỉnh sửa story
        if (!storyService.canEditStory(currentUser, existingStory)) {
            throw new ForbiddenException("You don't have permission to edit this story");
        }

        // Update fields from request
        existingStory.setContent(storyRequest.getContent());
        existingStory.setPrivacy(storyRequest.getPrivacy());

        Story updatedStory = storyService.updateStory(storyId, existingStory);
        StoryResponse storyResponse = mapToStoryResponse(updatedStory);
        return ResponseEntity.ok(WrapRes.success(storyResponse));
    }

    @Operation(summary = "Xóa story", description = "Xóa story", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa story thành công")
    })
    @DeleteMapping("/{storyId}")
    public ResponseEntity<WrapRes<?>> deleteStory(@PathVariable Long storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Story story = storyService.getStoryById(storyId).get();

        // Kiểm tra quyền xóa story
        if (!storyService.canDeleteStory(currentUser, story)) {
            throw new ForbiddenException("You don't have permission to delete this story");
        }

        storyService.deleteStory(storyId);
        return ResponseEntity.ok(WrapRes.success("Story deleted successfully"));
    }

    @Operation(summary = "Tìm kiếm stories theo content", description = "Tìm kiếm stories theo nội dung", responses = {
            @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    })
    @GetMapping("/search")
    public ResponseEntity<WrapRes<?>> searchStories(@RequestParam String content) {
        List<Story> stories = storyService.searchStoriesByContent(content);
        List<StoryResponse> storyResponses = stories.stream()
                .filter(story -> story.getPrivacy() == PrivacyLevel.PUBLIC) // Chỉ trả về public stories
                .map(this::mapToStoryResponse)
                .toList();
        return ResponseEntity.ok(WrapRes.success(storyResponses));
    }

    @Operation(summary = "Lấy stories của tôi", description = "Lấy danh sách stories của user hiện tại", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy stories thành công")
    })
    @GetMapping("/my-stories")
    public ResponseEntity<WrapRes<?>> getMyStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(page, size);
        Page<Story> stories = storyService.getStoriesByUser(currentUser, pageable);
        Page<StoryResponse> storyResponses = stories.map(this::mapToStoryResponse);

        return ResponseEntity.ok(WrapRes.success(storyResponses));
    }

    // Helper method to map Story entity to StoryResponse
    private StoryResponse mapToStoryResponse(Story story) {
        // Map user to UserResponse manually to avoid circular reference
        UserResponse userResponse = null;
        if (story.getUser() != null) {
            try {
                User user = story.getUser();
                userResponse = UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .roles(user.getRoles())
                        .dateOfBirth(user.getDateOfBirth())
                        .gender(user.getGender())
                        .avatarUrl(user.getAvatarUrl())
                        .coverUrl(user.getCoverUrl())
                        .bio(user.getBio())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .enabled(user.isEnabled())
                        .username(user.getUsername())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to map user for story {}: {}", story.getId(), e.getMessage());
            }
        }

        // Get media for story
        List<Media> mediaList = storyService.getStoryMedia(story.getId());
        List<MediaResponse> mediaResponses = mediaList.stream()
                .map(this::mapToMediaResponse)
                .toList();

        return StoryResponse.builder()
                .id(story.getId())
                .content(story.getContent())
                .privacy(story.getPrivacy())
                .user(userResponse)
                .media(mediaResponses)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }

    // Helper method to map Media entity to MediaResponse
    private MediaResponse mapToMediaResponse(Media media) {
        UserResponse uploadedByResponse = null;
        if (media.getUploadedBy() != null) {
            try {
                User uploadedBy = media.getUploadedBy();
                uploadedByResponse = UserResponse.builder()
                        .id(uploadedBy.getId())
                        .email(uploadedBy.getEmail())
                        .firstName(uploadedBy.getFirstName())
                        .lastName(uploadedBy.getLastName())
                        .roles(uploadedBy.getRoles())
                        .dateOfBirth(uploadedBy.getDateOfBirth())
                        .gender(uploadedBy.getGender())
                        .avatarUrl(uploadedBy.getAvatarUrl())
                        .coverUrl(uploadedBy.getCoverUrl())
                        .bio(uploadedBy.getBio())
                        .createdAt(uploadedBy.getCreatedAt())
                        .updatedAt(uploadedBy.getUpdatedAt())
                        .enabled(uploadedBy.isEnabled())
                        .username(uploadedBy.getUsername())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to map uploadedBy user for media {}: {}", media.getId(), e.getMessage());
                uploadedByResponse = null;
            }
        }

        return MediaResponse.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .mediaType(media.getMediaType())
                .originalFilename(media.getOriginalFilename())
                .fileSize(media.getFileSize())
                .uploadedBy(uploadedByResponse)
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .build();
    }
}