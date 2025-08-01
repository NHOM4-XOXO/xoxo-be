package com.nhom4.xoxo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.repository.MediaRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;

@Service
public class MediaService {
    
    @Autowired
    private MediaRepository mediaRepository;
    
    @Autowired
    private MediaRoomRepository mediaRoomRepository;
    
    // Lấy media của post
    public List<Media> getPostMedia(Long postId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    // Lấy media của comment
    public List<Media> getCommentMedia(Long commentId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            commentId, MediaRoomTargetType.COMMENT);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    // Thêm media cho post
    public void addMediaToPost(Long postId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));
        
        MediaRoom mediaRoom = MediaRoom.builder()
            .media(media)
            .targetId(postId)
            .targetType(MediaRoomTargetType.POST)
            .build();
        
        mediaRoomRepository.save(mediaRoom);
    }
    
    // Thêm media cho comment
    public void addMediaToComment(Long commentId, Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Media not found"));
        
        MediaRoom mediaRoom = MediaRoom.builder()
            .media(media)
            .targetId(commentId)
            .targetType(MediaRoomTargetType.COMMENT)
            .build();
        
        mediaRoomRepository.save(mediaRoom);
    }
    
    // Xóa media khỏi target
    public void removeMediaFromTarget(Long targetId, MediaRoomTargetType targetType, Long mediaId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(targetId, targetType);
        
        mediaRooms.stream()
            .filter(mr -> mr.getMedia().getId().equals(mediaId))
            .findFirst()
            .ifPresent(mediaRoomRepository::delete);
    }
} 