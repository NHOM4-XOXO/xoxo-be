package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostStatus;

@Repository
public interface SharePostRepository extends JpaRepository<SharePost, Long> {
    
    // Tìm shares theo original post
    List<SharePost> findByOriginalPost(Post originalPost);
    
    // Tìm shares theo original post và status
    List<SharePost> findByOriginalPostAndStatus(Post originalPost, PostStatus status);
    
    // Tìm shares theo sharer
    List<SharePost> findBySharer(User sharer);
    
    // Tìm shares theo original post và sharer
    List<SharePost> findByOriginalPostAndSharer(Post originalPost, User sharer);
    
    // Tìm shares public
    List<SharePost> findByIsPublicTrue();
    
    // Tìm shares theo sharer và public
    List<SharePost> findBySharerAndIsPublicTrue(User sharer);
    
    // Pagination cho shares public
    Page<SharePost> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Đếm shares theo original post
    @Query("SELECT COUNT(sp) FROM SharePost sp WHERE sp.originalPost = :originalPost")
    Long countByOriginalPost(@Param("originalPost") Post originalPost);
    
    // Đếm shares public theo original post
    @Query("SELECT COUNT(sp) FROM SharePost sp WHERE sp.originalPost = :originalPost AND sp.isPublic = true")
    Long countPublicSharesByOriginalPost(@Param("originalPost") Post originalPost);
    
    // Tìm shares theo content
    @Query("SELECT sp FROM SharePost sp WHERE sp.shareContent LIKE %:content%")
    List<SharePost> findByShareContentContaining(@Param("content") String content);
    
    // Kiểm tra user đã share post chưa
    @Query("SELECT COUNT(sp) > 0 FROM SharePost sp WHERE sp.originalPost = :originalPost AND sp.sharer = :sharer")
    boolean existsByOriginalPostAndSharer(@Param("originalPost") Post originalPost, @Param("sharer") User sharer);
} 