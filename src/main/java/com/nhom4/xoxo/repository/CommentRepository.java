package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostStatus;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Tìm comments theo post
    List<Comment> findByPost(Post post);
    
    // Tìm comments theo post và status
    List<Comment> findByPostAndStatus(Post post, PostStatus status);
    
    // Tìm comments theo author
    List<Comment> findByAuthor(User author);
    
    // Tìm comments theo post và author
    List<Comment> findByPostAndAuthor(Post post, User author);
    
    // Tìm top-level comments (không có parent)
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parentComment IS NULL")
    List<Comment> findTopLevelCommentsByPost(@Param("post") Post post);
    
    // Đếm số lượng replies của 1 comment
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentComment = :parentComment")
    Long countRepliesByParentComment(@Param("parentComment") Comment parentComment);

    // Tìm replies của 1 comment
    @Query("SELECT c FROM Comment c WHERE c.parentComment = :parentComment")
    List<Comment> findRepliesByParentComment(@Param("parentComment") Comment parentComment);
    

    
    // Pagination cho comments của post
    Page<Comment> findByPostOrderByCreatedAtDesc(Post post, Pageable pageable);
    
    // Đếm comments theo post
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post = :post")
    Long countByPost(@Param("post") Post post);
    
    // Đếm top-level comments theo post
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post = :post AND c.parentComment IS NULL")
    Long countTopLevelCommentsByPost(@Param("post") Post post);
    
    // Tìm comments theo content
    @Query("SELECT c FROM Comment c WHERE c.content LIKE %:content%")
    List<Comment> findByContentContaining(@Param("content") String content);

    // ===== Materialized Path helpers =====
    @Query(value = "SELECT COUNT(*) FROM comments c WHERE c.path LIKE CONCAT(:path, '/%')", nativeQuery = true)
    Long countDescendantsByPath(@Param("path") String path);

    @Query(value = "SELECT * FROM comments c WHERE c.path = :path OR c.path LIKE CONCAT(:path, '/%') ORDER BY c.path", nativeQuery = true)
    List<Comment> findSubtreeByPath(@Param("path") String path);

    @Query(value = "SELECT * FROM comments c WHERE c.path LIKE CONCAT(:path, '/%') ORDER BY c.path", nativeQuery = true)
    List<Comment> findDescendantsByPath(@Param("path") String path);

    @Query("SELECT c FROM Comment c WHERE c.rootId = :rootId ORDER BY c.path ASC")
    List<Comment> findThreadByRootId(@Param("rootId") Long rootId);
} 