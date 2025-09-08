package com.nhom4.xoxo.service;

import org.springframework.web.multipart.MultipartFile;
import com.nhom4.xoxo.enums.MediaType;

public interface CloudinaryService {
    // Legacy methods (keep for backward compatibility)
    String uploadImage(MultipartFile file, String folder); 
    String uploadImageAndGetUrl(MultipartFile file, String folder);
    boolean deleteImage(String publicId);
    
    // Enhanced methods for all media types
    String uploadMedia(MultipartFile file, String folder, MediaType mediaType);
    String uploadMediaAndGetUrl(MultipartFile file, String folder, MediaType mediaType);
    String buildCloudinaryUrl(String publicId, MediaType mediaType);
    boolean deleteMedia(String publicId);
    
    // Video-specific methods
    String uploadVideo(MultipartFile file, String folder);
    String uploadVideoAndGetUrl(MultipartFile file, String folder);
    
    // Audio-specific methods  
    String uploadAudio(MultipartFile file, String folder);
    String uploadAudioAndGetUrl(MultipartFile file, String folder);
    
    // File validation
    boolean isValidImageFile(MultipartFile file);
    boolean isValidVideoFile(MultipartFile file);
    boolean isValidAudioFile(MultipartFile file);
}