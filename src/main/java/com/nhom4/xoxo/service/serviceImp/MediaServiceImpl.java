package com.nhom4.xoxo.service.serviceImp;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.MediaType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.UnauthorizedException;
import com.nhom4.xoxo.repository.MediaRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.MediaService;

@Service
public class MediaServiceImpl implements MediaService {
    
   
    private final   MediaRepository mediaRepository;
    
   
    private final MediaRoomRepository mediaRoomRepository;
    
    private final CloudinaryService cloudinaryService;

    public MediaServiceImpl(MediaRepository mediaRepository, MediaRoomRepository mediaRoomRepository, CloudinaryService cloudinaryService) {
        this.mediaRepository = mediaRepository;
        this.mediaRoomRepository = mediaRoomRepository;
        this.cloudinaryService = cloudinaryService;
    }
    
    @Override
    @Transactional
    public Media uploadMedia(MultipartFile file, MediaType mediaType, User user) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
        
            String mediaUrl = "/uploads/" + uniqueFilename;
            
            Media media = Media.builder()
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .originalFilename(originalFilename)
                .fileSize(file.getSize())
                .uploadedBy(user)
                .build();
            
            cloudinaryService.uploadImage(file, "media");
            return mediaRepository.save(media);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload media: " + e.getMessage());
        }
    }
    
    @Override
    public List<Media> uploadMultipleMedia(List<MultipartFile> files, MediaType mediaType, User user) {
        return files.stream()
            .map(file -> uploadMedia(file, mediaType, user))
            .collect(Collectors.toList());
    }
    
    @Override
    public Media getMediaById(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
            .orElseThrow(() -> new NotFoundException("Media not found with id: " + mediaId));

        media.setMediaUrl(cloudinaryService.buildCloudinaryUrl(media.getMediaUrl()));

        return media;
    }
    
    @Override
    public List<Media> getMediaByUser(User user) {
        List<Media> mediaList = mediaRepository.findByUploadedBy(user);
        mediaList.forEach(media -> media.setMediaUrl(cloudinaryService.buildCloudinaryUrl(media.getMediaUrl())));
        return mediaList;
    }
    
    @Override
    public void deleteMedia(Long mediaId, User user) {
        Media media = getMediaById(mediaId);
        
        // Check if user owns the media
        if (!media.getUploadedBy().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own media");
        }
        
        // TODO: Delete from cloud storage
        mediaRepository.delete(media);
    }
    
    @Override
    public List<Media> getPostMedia(Long postId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Media> getCommentMedia(Long commentId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            commentId, MediaRoomTargetType.COMMENT);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    @Override
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
    
    @Override
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
    
    @Override
    public void removeMediaFromTarget(Long targetId, MediaRoomTargetType targetType, Long mediaId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(targetId, targetType);
        
        mediaRooms.stream()
            .filter(mr -> mr.getMedia().getId().equals(mediaId))
            .findFirst()
            .ifPresent(mediaRoomRepository::delete);
    }
} 