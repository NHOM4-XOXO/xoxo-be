package com.nhom4.xoxo.service;

import java.util.List;
import java.util.Optional;

import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.User;

public interface PostService {
    
    // Tạo post mới
    Post createPost(Post post);
    
    // Lấy post theo ID
    Optional<Post> getPostById(Long postId);
    
    // Lấy tất cả posts public
    List<Post> getPublicPosts();
    
    // Lấy posts theo author
    List<Post> getPostsByAuthor(User author);
    
    // Lấy media của post
    List<Media> getPostMedia(Long postId);
    
    // Lấy comments của post
    List<Comment> getPostComments(Long postId);
    
    // Lấy shares của post
    List<SharePost> getPostShares(Long postId);
    
    // Thêm media cho post
    void addMediaToPost(Long postId, Long mediaId);
    
    // Xóa media khỏi post
    void removeMediaFromPost(Long postId, Long mediaId);
    
    // Cập nhật post
    Post updatePost(Long postId, Post updatedPost);
    
    // Xóa post
    void deletePost(Long postId);
    
    // Tăng like count
    void incrementLikeCount(Long postId);
    
    // Tăng comment count
    void incrementCommentCount(Long postId);
    
    // Tăng share count
    void incrementShareCount(Long postId);
    
    // Tăng view count
    void incrementViewCount(Long postId);
} 