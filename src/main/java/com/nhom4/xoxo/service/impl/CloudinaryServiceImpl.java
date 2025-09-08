package com.nhom4.xoxo.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nhom4.xoxo.enums.MediaType;
import com.nhom4.xoxo.exception.PostException;
import com.nhom4.xoxo.service.CloudinaryService;

@Service
public class CloudinaryServiceImpl implements CloudinaryService{
    @Autowired
    private Cloudinary cloudinary;
    
    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new PostException("File không được để trống");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folder));
            return uploadResult.get("public_id").toString(); // trả về publicId (giữ lại để backward compatibility)
        } catch (IOException e) {
            throw new PostException("Upload image to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadImageAndGetUrl(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new PostException("File không được để trống");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folder));
            
            // Lấy secure URL (HTTPS)
            String secureUrl = uploadResult.get("secure_url").toString();
            
            // Lưu public_id vào database để có thể xóa sau này
            // Bạn có thể lưu cả public_id và URL, hoặc chỉ lưu URL
            return secureUrl;
            
        } catch (IOException e) {
            throw new PostException("Upload image to Cloudinary failed: " + e.getMessage(), e);
        }
    }


    
    @Override
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Nếu input là full URL, extract public_id
            String actualPublicId = publicId;
            if (publicId.startsWith("https://res.cloudinary.com/")) {
                // Extract public_id from URL: https://res.cloudinary.com/cloud_name/image/upload/public_id
                String[] parts = publicId.split("/image/upload/");
                if (parts.length > 1) {
                    actualPublicId = parts[1];
                }
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(actualPublicId, ObjectUtils.emptyMap());
            
            // Kiểm tra kết quả xóa
            String resultValue = result.get("result").toString();
            return "ok".equals(resultValue);
            
        } catch (Exception e) {
            throw new PostException("Delete image from Cloudinary failed: " + e.getMessage(), e);
        }
    }

    // ==================== Enhanced Media Upload Methods ====================

    @Override
    public String uploadMedia(MultipartFile file, String folder, MediaType mediaType) {
        if (file == null || file.isEmpty()) {
            throw new PostException("File không được để trống");
        }

        // Validate file type
        if (!isValidFile(file, mediaType)) {
            throw new PostException("File type không hợp lệ cho " + mediaType);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadParams = ObjectUtils.asMap("folder", folder);
            
            // Configure upload parameters based on media type
            switch (mediaType) {
                case VIDEO:
                    uploadParams.put("resource_type", "video");
                    uploadParams.put("quality", "auto");
                    uploadParams.put("format", "mp4"); // Convert to mp4 for compatibility
                    break;
                case AUDIO:
                    uploadParams.put("resource_type", "video"); // Audio uses video resource type in Cloudinary
                    break;
                case IMAGE:
                default:
                    uploadParams.put("resource_type", "image");
                    uploadParams.put("quality", "auto");
                    uploadParams.put("format", "webp"); // Convert to webp for better compression
                    break;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return uploadResult.get("public_id").toString();
            
        } catch (IOException e) {
            throw new PostException("Upload " + mediaType.name().toLowerCase() + " to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadMediaAndGetUrl(MultipartFile file, String folder, MediaType mediaType) {
        String publicId = uploadMedia(file, folder, mediaType);
        return buildCloudinaryUrl(publicId, mediaType);
    }

    @Override
    public String buildCloudinaryUrl(String publicId, MediaType mediaType) {
        if (publicId == null) {
            return null;
        }
        
        String value = publicId.trim();
        // If it's already a full URL, return as-is
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        
        // Build URL based on media type
        String resourceType = "image";
        switch (mediaType) {
            case VIDEO:
            case AUDIO:
                resourceType = "video";
                break;
            case IMAGE:
            default:
                resourceType = "image";
                break;
        }
        
        return "https://res.cloudinary.com/" + cloudName + "/" + resourceType + "/upload/" + value;
    }

    @Override
    public String uploadVideo(MultipartFile file, String folder) {
        return uploadMedia(file, folder, MediaType.VIDEO);
    }

    @Override
    public String uploadVideoAndGetUrl(MultipartFile file, String folder) {
        return uploadMediaAndGetUrl(file, folder, MediaType.VIDEO);
    }

    @Override
    public String uploadAudio(MultipartFile file, String folder) {
        return uploadMedia(file, folder, MediaType.AUDIO);
    }

    @Override
    public String uploadAudioAndGetUrl(MultipartFile file, String folder) {
        return uploadMediaAndGetUrl(file, folder, MediaType.AUDIO);
    }

    @Override
    public boolean deleteMedia(String publicId) {
        // Use the same logic as deleteImage for now
        return deleteImage(publicId);
    }

    // ==================== File Validation Methods ====================

    @Override
    public boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("image/") && 
               (contentType.equals("image/jpeg") || 
                contentType.equals("image/jpg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/gif") || 
                contentType.equals("image/webp"));
    }

    @Override
    public boolean isValidVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("video/") && 
               (contentType.equals("video/mp4") || 
                contentType.equals("video/avi") || 
                contentType.equals("video/mov") || 
                contentType.equals("video/webm") || 
                contentType.equals("video/quicktime"));
    }

    @Override
    public boolean isValidAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("audio/") && 
               (contentType.equals("audio/mp3") || 
                contentType.equals("audio/wav") || 
                contentType.equals("audio/aac") || 
                contentType.equals("audio/ogg"));
    }

    private boolean isValidFile(MultipartFile file, MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                return isValidImageFile(file);
            case VIDEO:
                return isValidVideoFile(file);
            case AUDIO:
                return isValidAudioFile(file);
            default:
                return false;
        }
    }
}