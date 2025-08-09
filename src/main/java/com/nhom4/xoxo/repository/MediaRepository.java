package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaType;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    @Query("SELECT m FROM Media m WHERE m.id = :mediaId")
    Media findByIdWithCloudinaryUrl(@Param("mediaId") Long mediaId);
    
    // Tìm media theo type
    List<Media> findByMediaType(MediaType mediaType);
    
    // Tìm media theo URL
    List<Media> findByMediaUrl(String mediaUrl);
    
    // Tìm media theo URL chứa pattern
    @Query("SELECT m FROM Media m WHERE m.mediaUrl LIKE %:pattern%")
    List<Media> findByMediaUrlContaining(@Param("pattern") String pattern);
    
    // Tìm media theo type và URL pattern
    @Query("SELECT m FROM Media m WHERE m.mediaType = :mediaType AND m.mediaUrl LIKE %:pattern%")
    List<Media> findByMediaTypeAndMediaUrlContaining(@Param("mediaType") MediaType mediaType, @Param("pattern") String pattern);
    
    // Kiểm tra media URL đã tồn tại chưa
    boolean existsByMediaUrl(String mediaUrl);
    
    // Đếm media theo type
    @Query("SELECT COUNT(m) FROM Media m WHERE m.mediaType = :mediaType")
    Long countByMediaType(@Param("mediaType") MediaType mediaType);
    
    // Tìm media theo user upload
    List<Media> findByUploadedBy(User uploadedBy);
} 