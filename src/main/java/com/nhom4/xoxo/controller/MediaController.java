package com.nhom4.xoxo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.dto.res.UserResponse;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaType;
import com.nhom4.xoxo.service.MediaService;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "APIs for media upload and management")
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    @Operation(summary = "Upload media", description = "Upload file media (ảnh/video) lên server", responses = {
            @ApiResponse(responseCode = "200", description = "Upload media thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi upload file")
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<?>> uploadMedia(
            @Parameter(description = "File media cần upload", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Loại media (IMAGE/VIDEO)", required = true)
            @RequestParam("mediaType") MediaType mediaType) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email);

            Media uploadedMedia = mediaService.uploadMedia(file, mediaType, currentUser);
            MediaResponse mediaResponse = mapToMediaResponse(uploadedMedia);
            return ResponseEntity.ok(WrapRes.success(mediaResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(WrapRes.error("Upload failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Upload nhiều media", description = "Upload nhiều file media cùng lúc", responses = {
            @ApiResponse(responseCode = "200", description = "Upload media thành công"),
            @ApiResponse(responseCode = "400", description = "File không hợp lệ")
    })
    @PostMapping(value = "/upload-multiple", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<?>> uploadMultipleMedia(
            @Parameter(description = "Danh sách file media cần upload", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "Loại media (IMAGE/VIDEO)", required = true)
            @RequestParam("mediaType") MediaType mediaType) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User currentUser = userService.findByEmail(email);

            List<Media> uploadedMediaList = mediaService.uploadMultipleMedia(files, mediaType, currentUser);
            List<MediaResponse> mediaResponses = uploadedMediaList.stream()
                .map(this::mapToMediaResponse)
                .toList();
            return ResponseEntity.ok(WrapRes.success(mediaResponses));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(WrapRes.error("Upload failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Lấy media theo ID", description = "Lấy thông tin media theo ID", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy media thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy media")
    })
    @GetMapping("/{mediaId}")
    public ResponseEntity<WrapRes<?>> getMediaById(@PathVariable Long mediaId) {
        Media media = mediaService.getMediaById(mediaId);
        MediaResponse mediaResponse = mapToMediaResponse(media);
        return ResponseEntity.ok(WrapRes.success(mediaResponse));
    }

    @Operation(summary = "Lấy media của user", description = "Lấy danh sách media của user hiện tại", responses = {
            @ApiResponse(responseCode = "200", description = "Lấy media thành công")
    })
    @GetMapping("/my-media")
    public ResponseEntity<WrapRes<?>> getMyMedia() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        List<Media> mediaList = mediaService.getMediaByUser(currentUser);
        List<MediaResponse> mediaResponses = mediaList.stream()
            .map(this::mapToMediaResponse)
            .toList();
        return ResponseEntity.ok(WrapRes.success(mediaResponses));
    }

    @Operation(summary = "Xóa media", description = "Xóa media theo ID", responses = {
            @ApiResponse(responseCode = "200", description = "Xóa media thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy media")
    })
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<WrapRes<?>> deleteMedia(@PathVariable Long mediaId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User currentUser = userService.findByEmail(email);

        mediaService.deleteMedia(mediaId, currentUser);
        return ResponseEntity.ok(WrapRes.success("Media deleted successfully"));
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
                log.warn("Failed to map uploadedBy for media {}: {}", media.getId(), e.getMessage());   
            }
        }
        
        return MediaResponse.builder()
            .id(media.getId())
            .mediaUrl(cloudinaryService.buildCloudinaryUrl(media.getMediaUrl(), media.getMediaType()))
            .mediaType(media.getMediaType())
            .originalFilename(media.getOriginalFilename())
            .fileSize(media.getFileSize())
            .uploadedBy(uploadedByResponse)
            .createdAt(media.getCreatedAt())
            .updatedAt(media.getUpdatedAt())
            .build();
    }
} 