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
    
    // Tìm replies của 1 comment
    List<Comment> findByParentComment(Comment parentComment);
    
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
} 