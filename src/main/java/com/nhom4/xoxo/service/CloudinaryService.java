package com.nhom4.xoxo.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadImage(MultipartFile file, String folder); 
    String buildCloudinaryUrl(String publicId);
    
    // Thêm method mới để upload và trả về full URL
    String uploadImageAndGetUrl(MultipartFile file, String folder);
    
    // Method để xóa image từ Cloudinary
    boolean deleteImage(String publicId);
}