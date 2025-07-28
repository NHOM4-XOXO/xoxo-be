package com.nhom4.xoxo.service.serviceImp;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.nhom4.xoxo.service.CloudinaryService;

@Service
public class CloudinaryServiceImpl implements CloudinaryService{
    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folder));
            return uploadResult.get("public_id").toString(); // trả về publicId
        } catch (IOException e) {
            throw new RuntimeException("Upload image to Cloudinary failed", e);
        }
    }

    @Override
    public String buildCloudinaryUrl(String publicId) {
      
        String cloudName ="dv9tvgnsz";
        if(publicId == null){
            return null;
        }
        if(publicId.contains("avatars")||publicId.contains("covers")){
            return "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId;
        }
        
        return publicId;
    }
}
