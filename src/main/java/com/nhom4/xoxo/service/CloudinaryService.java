package com.nhom4.xoxo.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    String uploadImage(MultipartFile file, String folder); 
    String buildCloudinaryUrl(String publicId);
    
}