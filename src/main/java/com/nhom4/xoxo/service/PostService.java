package com.nhom4.xoxo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.repository.CommentRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.SharePostRepository;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private SharePostRepository sharePostRepository;
    
    @Autowired
    private MediaRoomRepository mediaRoomRepository;
    
    // Tạo post mới
    public Post createPost(Post post) {
        return postRepository.save(post);
    }
    
    // Lấy post theo ID
    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
    }
    
    // Lấy tất cả posts public
    public List<Post> getPublicPosts() {
        return postRepository.findByIsPublicTrue();
    }
    
    // Lấy posts theo author
    public List<Post> getPostsByAuthor(User author) {
        return postRepository.findByAuthorAndIsPublicTrue(author);
    }
    
    // Lấy media của post
    public List<Media> getPostMedia(Long postId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    // Lấy comments của post
    public List<Comment> getPostComments(Long postId) {
        Post post = getPostById(postId);
        return commentRepository.findTopLevelCommentsByPost(post);
    }
    
    // Lấy shares của post
    public List<SharePost> getPostShares(Long postId) {
        Post post = getPostById(postId);
        return sharePostRepository.findByOriginalPost(post);
    }
    
    // Thêm media cho post
    public void addMediaToPost(Long postId, Long mediaId) {
        MediaRoom mediaRoom = MediaRoom.builder()
            .targetId(postId)
            .targetType(MediaRoomTargetType.POST)
            .build();
        
        // Cần set Media entity
        // mediaRoom.setMedia(mediaRepository.findById(mediaId).orElseThrow());
        
        mediaRoomRepository.save(mediaRoom);
    }
    
    // Xóa media khỏi post
    public void removeMediaFromPost(Long postId, Long mediaId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        mediaRooms.stream()
            .filter(mr -> mr.getMedia().getId().equals(mediaId))
            .findFirst()
            .ifPresent(mediaRoomRepository::delete);
    }
    
    // Cập nhật post
    public Post updatePost(Long postId, Post updatedPost) {
        Post existingPost = getPostById(postId);
        
        existingPost.setContent(updatedPost.getContent());
        existingPost.setStatus(updatedPost.getStatus());
        existingPost.setType(updatedPost.getType());
        existingPost.setLocation(updatedPost.getLocation());
        existingPost.setHashtags(updatedPost.getHashtags());
        existingPost.setPublic(updatedPost.isPublic());
        existingPost.setAllowComments(updatedPost.isAllowComments());
        existingPost.setAllowLikes(updatedPost.isAllowLikes());
        existingPost.setAllowShares(updatedPost.isAllowShares());
        
        return postRepository.save(existingPost);
    }
    
    // Xóa post
    public void deletePost(Long postId) {
        Post post = getPostById(postId);
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }
    
    // Tăng like count
    public void incrementLikeCount(Long postId) {
        Post post = getPostById(postId);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }
    
    // Tăng comment count
    public void incrementCommentCount(Long postId) {
        Post post = getPostById(postId);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }
    
    // Tăng share count
    public void incrementShareCount(Long postId) {
        Post post = getPostById(postId);
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
    }
    
    // Tăng view count
    public void incrementViewCount(Long postId) {
        Post post = getPostById(postId);
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
} 