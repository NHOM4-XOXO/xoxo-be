package com.nhom4.xoxo.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.MediaType;

public interface MediaService {
    
    // Upload methods
    Media uploadMedia(MultipartFile file, MediaType mediaType, User user);
    List<Media> uploadMultipleMedia(List<MultipartFile> files, MediaType mediaType, User user);
    
    // CRUD methods
    Media getMediaById(Long mediaId);
    List<Media> getMediaByUser(User user);
    void deleteMedia(Long mediaId, User user);
    
    // Lấy media của post
    List<Media> getPostMedia(Long postId);
    
    // Lấy media của comment
    List<Media> getCommentMedia(Long commentId);
    
    // Thêm media cho post
    void addMediaToPost(Long postId, Long mediaId);
    
    // Thêm media cho comment
    void addMediaToComment(Long commentId, Long mediaId);
    
    // Xóa media khỏi target
    void removeMediaFromTarget(Long targetId, MediaRoomTargetType targetType, Long mediaId);
} 