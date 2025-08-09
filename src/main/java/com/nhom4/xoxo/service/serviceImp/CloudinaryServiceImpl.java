package com.nhom4.xoxo.service.serviceImp;

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
            return uploadResult.get("public_id").toString(); // trả về publicId
        } catch (IOException e) {
            throw new PostException("Upload image to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String buildCloudinaryUrl(String publicId) {
        if(publicId == null){
            return null;
        }
        if(publicId.contains("avatars")||publicId.contains("covers")||publicId.contains("media")){
            return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId;
        }
        
        return publicId;
    }
}
