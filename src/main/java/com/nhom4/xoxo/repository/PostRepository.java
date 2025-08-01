package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findById(Long postId);
    
    // Tìm posts theo author
    List<Post> findByAuthor(User author);
    
    // Tìm posts theo author và status
    List<Post> findByAuthorAndStatus(User author, PostStatus status);
    
    // Tìm posts theo status
    List<Post> findByStatus(PostStatus status);
    
    // Tìm posts theo type
    List<Post> findByType(PostType type);
    
    // Tìm posts theo parent post (replies)
    List<Post> findByParentPost(Post parentPost);
    
    // Tìm posts public
    @Query("SELECT p FROM Post p WHERE p.isPublic = true AND p.status = 'ACTIVE'")
    List<Optional<Post>> findByIsPublicTrue();
    
    // Tìm posts theo author và public
    List<Post> findByAuthorAndIsPublicTrue(User author);
    
    // Pagination cho posts public
    Page<Post> findByIsPublicTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Tìm posts theo hashtags
    @Query("SELECT p FROM Post p WHERE p.hashtags LIKE %:hashtag%")
    List<Post> findByHashtagsContaining(@Param("hashtag") String hashtag);
    
    // Tìm posts theo content
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:content%")
    List<Post> findByContentContaining(@Param("content") String content);
    
    // Tìm posts theo location
    @Query("SELECT p FROM Post p WHERE p.location LIKE %:location%")
    List<Post> findByLocationContaining(@Param("location") String location);
    
    // Đếm posts theo author
    @Query("SELECT COUNT(p) FROM Post p WHERE p.author = :author")
    Long countByAuthor(@Param("author") User author);
    
    // Đếm posts public theo author
    @Query("SELECT COUNT(p) FROM Post p WHERE p.author = :author AND p.isPublic = true")
    Long countPublicPostsByAuthor(@Param("author") User author);
} 