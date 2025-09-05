package com.nhom4.xoxo.service;

import java.util.List;
import java.util.Optional;

import com.nhom4.xoxo.dto.res.CommentItemResponse;
import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.dto.res.PostWithMediaResponse;
import com.nhom4.xoxo.dto.res.SharePostItemResponse;
import com.nhom4.xoxo.dto.res.UserLikeResponse;
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

    // Lấy post theo ID
    Optional<PostItemResponse> getPostItemById(Long postId);
    
    // Lấy tất cả posts public
    List<PostWithMediaResponse> getPublicPostsWithMedia();
    
    List<PostItemResponse> getPublicPosts();

    // Lấy tất cả posts
    List<PostItemResponse> getAllPosts();
    
    // Lấy posts theo author
    List<PostItemResponse> getPostsByAuthor(User author);
    
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

    // Like/Unlike post, trả về true nếu đã like sau thao tác
    boolean toggleLike(Long postId, User user);

    // Tạo comment
    CommentItemResponse addComment(Long postId, User author, String content, Long parentCommentId);

    // Tạo share
    SharePostItemResponse sharePost(Long postId, User sharer, String shareContent);

    // Danh sách users like post
    List<UserLikeResponse> getUsersLikedPost(Long postId);

    // Danh sách shares của post (DTO)
    List<SharePostItemResponse> getSharesOfPost(Long postId);

    // Danh sách comments top-level của post (DTO)
    List<CommentItemResponse> getCommentsOfPost(Long postId);
} 