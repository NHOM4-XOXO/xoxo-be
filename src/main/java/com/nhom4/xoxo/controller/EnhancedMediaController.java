package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.res.MediaResponse;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaType;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.MediaService;
import com.nhom4.xoxo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enhanced Media", description = "Enhanced media upload with video support")
public class EnhancedMediaController {

    private final MediaService mediaService;
    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findByEmail(email);
    }

    @Operation(summary = "Upload video for post", description = "Upload video file with proper validation and processing")
    @ApiResponse(responseCode = "200", description = "Video uploaded successfully")
    @PostMapping(value = "/upload-video", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<MediaResponse>> uploadVideo(
            @Parameter(description = "Video file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Attempting to upload video: {}, size: {} bytes, contentType: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Validate video file
            if (!cloudinaryService.isValidVideoFile(file)) {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("INVALID_FILE", "File không phải là video hợp lệ. Chỉ hỗ trợ: MP4, AVI, MOV, WEBM"));
            }

            // Check file size (max 50MB for videos)
            if (file.getSize() > 50 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("FILE_TOO_LARGE", "Video không được vượt quá 50MB"));
            }

            User currentUser = getCurrentUser();
            Media uploadedMedia = mediaService.uploadMedia(file, MediaType.VIDEO, currentUser);
            
            MediaResponse response = MediaResponse.builder()
                .id(uploadedMedia.getId())
                .mediaUrl(uploadedMedia.getMediaUrl())
                .mediaType(uploadedMedia.getMediaType())
                .originalFilename(uploadedMedia.getOriginalFilename())
                .fileSize(uploadedMedia.getFileSize())
                .uploadedBy(com.nhom4.xoxo.dto.res.UserResponse.builder()
                    .id(currentUser.getId())
                    .firstName(currentUser.getFirstName())
                    .lastName(currentUser.getLastName())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .build())
                .createdAt(uploadedMedia.getCreatedAt())
                .updatedAt(uploadedMedia.getUpdatedAt())
                .build();

            log.info("Video uploaded successfully: ID={}, URL={}", uploadedMedia.getId(), uploadedMedia.getMediaUrl());
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error uploading video: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(WrapRes.error("UPLOAD_FAILED", "Upload video thất bại: " + e.getMessage()));
        }
    }

    @Operation(summary = "Upload image for post", description = "Upload image file with validation")
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<MediaResponse>> uploadImage(
            @Parameter(description = "Image file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        try {
            log.info("Attempting to upload image: {}, size: {} bytes, contentType: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Validate image file
            if (!cloudinaryService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("INVALID_FILE", "File không phải là ảnh hợp lệ. Chỉ hỗ trợ: JPG, PNG, GIF, WEBP"));
            }

            // Check file size (max 10MB for images)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("FILE_TOO_LARGE", "Ảnh không được vượt quá 10MB"));
            }

            User currentUser = getCurrentUser();
            Media uploadedMedia = mediaService.uploadMedia(file, MediaType.IMAGE, currentUser);
            
            MediaResponse response = MediaResponse.builder()
                .id(uploadedMedia.getId())
                .mediaUrl(uploadedMedia.getMediaUrl())
                .mediaType(uploadedMedia.getMediaType())
                .originalFilename(uploadedMedia.getOriginalFilename())
                .fileSize(uploadedMedia.getFileSize())
                .uploadedBy(com.nhom4.xoxo.dto.res.UserResponse.builder()
                    .id(currentUser.getId())
                    .firstName(currentUser.getFirstName())
                    .lastName(currentUser.getLastName())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .build())
                .createdAt(uploadedMedia.getCreatedAt())
                .updatedAt(uploadedMedia.getUpdatedAt())
                .build();

            log.info("Image uploaded successfully: ID={}, URL={}", uploadedMedia.getId(), uploadedMedia.getMediaUrl());
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(WrapRes.error("UPLOAD_FAILED", "Upload ảnh thất bại: " + e.getMessage()));
        }
    }

    @Operation(summary = "Auto-detect and upload media", description = "Auto-detect file type and upload accordingly")
    @PostMapping(value = "/upload-auto", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<MediaResponse>> uploadAutoDetect(
            @Parameter(description = "Media file to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        try {
            String contentType = file.getContentType();
            log.info("Auto-detecting file type: {}, contentType: {}", file.getOriginalFilename(), contentType);

            MediaType mediaType;
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    mediaType = MediaType.IMAGE;
                } else if (contentType.startsWith("video/")) {
                    mediaType = MediaType.VIDEO;
                } else if (contentType.startsWith("audio/")) {
                    mediaType = MediaType.AUDIO;
                } else {
                    return ResponseEntity.badRequest()
                        .body(WrapRes.error("UNSUPPORTED_TYPE", "File type không được hỗ trợ: " + contentType));
                }
            } else {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("UNKNOWN_TYPE", "Không thể xác định loại file"));
            }

            // Validate file size based on type
            long maxSize = switch (mediaType) {
                case VIDEO -> 100 * 1024 * 1024; // 100MB for videos
                case IMAGE -> 10 * 1024 * 1024;  // 10MB for images
                case AUDIO -> 20 * 1024 * 1024;  // 20MB for audio
            };

            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                    .body(WrapRes.error("FILE_TOO_LARGE", 
                        String.format("File quá lớn. Giới hạn cho %s là %dMB", 
                            mediaType.name(), maxSize / (1024 * 1024))));
            }

            User currentUser = getCurrentUser();
            Media uploadedMedia = mediaService.uploadMedia(file, mediaType, currentUser);
            
            MediaResponse response = MediaResponse.builder()
                .id(uploadedMedia.getId())
                .mediaUrl(uploadedMedia.getMediaUrl())
                .mediaType(uploadedMedia.getMediaType())
                .originalFilename(uploadedMedia.getOriginalFilename())
                .fileSize(uploadedMedia.getFileSize())
                .uploadedBy(com.nhom4.xoxo.dto.res.UserResponse.builder()
                    .id(currentUser.getId())
                    .firstName(currentUser.getFirstName())
                    .lastName(currentUser.getLastName())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .build())
                .createdAt(uploadedMedia.getCreatedAt())
                .updatedAt(uploadedMedia.getUpdatedAt())
                .build();

            log.info("Media uploaded successfully: ID={}, Type={}, URL={}", 
                uploadedMedia.getId(), mediaType, uploadedMedia.getMediaUrl());
            return ResponseEntity.ok(WrapRes.success(response));
            
        } catch (Exception e) {
            log.error("Error uploading media: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(WrapRes.error("UPLOAD_FAILED", "Upload thất bại: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get supported file types", description = "Get list of supported file types and size limits")
    @GetMapping("/supported-types")
    public ResponseEntity<WrapRes<Map<String, Object>>> getSupportedTypes() {
        Map<String, Object> supportedTypes = Map.of(
            "image", Map.of(
                "types", List.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"),
                "maxSizeMB", 10,
                "description", "Ảnh JPG, PNG, GIF, WEBP"
            ),
            "video", Map.of(
                "types", List.of("video/mp4", "video/avi", "video/mov", "video/webm", "video/quicktime"),
                "maxSizeMB", 100,
                "description", "Video MP4, AVI, MOV, WEBM"
            ),
            "audio", Map.of(
                "types", List.of("audio/mp3", "audio/wav", "audio/aac", "audio/ogg"),
                "maxSizeMB", 20,
                "description", "Audio MP3, WAV, AAC, OGG"
            )
        );

        return ResponseEntity.ok(WrapRes.success(supportedTypes));
    }

    @Operation(summary = "Validate file before upload", description = "Check if file is valid for upload")
    @PostMapping(value = "/validate", consumes = "multipart/form-data")
    public ResponseEntity<WrapRes<Map<String, Object>>> validateFile(
            @RequestParam("file") MultipartFile file) {
        
        try {
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            long fileSize = file.getSize();

            Map<String, Object> validation = Map.of(
                "filename", originalFilename,
                "contentType", contentType,
                "sizeMB", fileSize / (1024.0 * 1024.0),
                "isValidImage", cloudinaryService.isValidImageFile(file),
                "isValidVideo", cloudinaryService.isValidVideoFile(file),
                "isValidAudio", cloudinaryService.isValidAudioFile(file)
            );

            return ResponseEntity.ok(WrapRes.success(validation));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(WrapRes.error("VALIDATION_FAILED", "Validation thất bại: " + e.getMessage()));
        }
    }
}













