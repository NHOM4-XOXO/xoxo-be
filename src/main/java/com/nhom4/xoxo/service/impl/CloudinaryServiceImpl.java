package com.nhom4.xoxo.service.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
            
            // Lấy public_id để có thể xóa sau này nếu cần
            String publicId = uploadResult.get("public_id").toString();
            
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
    public String buildCloudinaryUrl(String publicId) {
        if (publicId == null) {
            return null;
        }
        String value = publicId.trim();
        // If it's already a full URL, return as-is (idempotent)
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        // Treat input as Cloudinary publicId
        return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + value;
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
}