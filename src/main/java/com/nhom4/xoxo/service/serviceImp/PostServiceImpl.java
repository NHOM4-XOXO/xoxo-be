package com.nhom4.xoxo.service.serviceImp;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.CommentRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.SharePostRepository;
import com.nhom4.xoxo.service.PostService;

@Service
public class PostServiceImpl implements PostService {
    
    
    private final PostRepository postRepository;
    
    
    private final CommentRepository commentRepository;
    
    
    private final SharePostRepository sharePostRepository;
    
 
    private final MediaRoomRepository mediaRoomRepository;

    public PostServiceImpl(PostRepository postRepository, CommentRepository commentRepository, SharePostRepository sharePostRepository, MediaRoomRepository mediaRoomRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.sharePostRepository = sharePostRepository;
        this.mediaRoomRepository = mediaRoomRepository;
    }
    
    @Override
    public Post createPost(Post post) {
        return postRepository.save(post);
    }
    
    @Override
    public Optional<Post> getPostById(Long postId) {
        Optional<Post> post = postRepository.findById(postId);
        if (post.isEmpty()) {
            throw new NotFoundException("Post not found");
        }
        return post;    
    }
        
           
    
    @Override
    public List<Post> getPublicPosts() {
        List<Optional<Post>> posts = postRepository.findByIsPublicTrue();
        return posts.stream()
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Post> getPostsByAuthor(User author) {
        return postRepository.findByAuthorAndIsPublicTrue(author);
    }
    
    @Override
    public List<Media> getPostMedia(Long postId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        return mediaRooms.stream()
            .map(MediaRoom::getMedia)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Comment> getPostComments(Long postId) {
        Post post = getPostById(postId).get();
        return commentRepository.findTopLevelCommentsByPost(post);
    }
    
    @Override
    public List<SharePost> getPostShares(Long postId) {
        Post post = getPostById(postId).get();
        return sharePostRepository.findByOriginalPost(post);
    }
    
    @Override
    public void addMediaToPost(Long postId, Long mediaId) {
        MediaRoom mediaRoom = MediaRoom.builder()
            .targetId(postId)
            .targetType(MediaRoomTargetType.POST)
            .build();
        
        mediaRoomRepository.save(mediaRoom);
    }
    
    @Override
    public void removeMediaFromPost(Long postId, Long mediaId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
            postId, MediaRoomTargetType.POST);
        
        mediaRooms.stream()
            .filter(mr -> mr.getMedia().getId().equals(mediaId))
            .findFirst()
            .ifPresent(mediaRoomRepository::delete);
    }
    
    @Override
    public Post updatePost(Long postId, Post updatedPost) {
        Post existingPost = getPostById(postId).get();
        
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
    
    @Override
    public void deletePost(Long postId) {
        Post post = getPostById(postId).get();
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }
    
    @Override
    public void incrementLikeCount(Long postId) {
        Post post = getPostById(postId).get();
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    public void incrementCommentCount(Long postId) {
        Post post = getPostById(postId).get();
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    public void incrementShareCount(Long postId) {
        Post post = getPostById(postId).get();
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    public void incrementViewCount(Long postId) {
        Post post = getPostById(postId).get();
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
} 