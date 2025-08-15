package com.nhom4.xoxo.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.dto.res.CommentItemResponse;
import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.dto.res.SharePostItemResponse;
import com.nhom4.xoxo.dto.res.UserLikeResponse;
import com.nhom4.xoxo.entity.Comment;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.SharePost;
import com.nhom4.xoxo.entity.PostLike;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.MediaRoomTargetType;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.CommentRepository;
import com.nhom4.xoxo.repository.MediaRoomRepository;
import com.nhom4.xoxo.repository.MediaRepository;
import com.nhom4.xoxo.repository.PostRepository;
import com.nhom4.xoxo.repository.SharePostRepository;
import com.nhom4.xoxo.repository.PostLikeRepository;
import com.nhom4.xoxo.service.CloudinaryService;
import com.nhom4.xoxo.service.PostService;
import com.nhom4.xoxo.service.NotificationService;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    private final CommentRepository commentRepository;

    private final SharePostRepository sharePostRepository;

    private final MediaRoomRepository mediaRoomRepository;
    private final MediaRepository mediaRepository;
    private final PostLikeRepository postLikeRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    public PostServiceImpl(PostRepository postRepository, CommentRepository commentRepository,
            SharePostRepository sharePostRepository, MediaRoomRepository mediaRoomRepository,
            MediaRepository mediaRepository, PostLikeRepository postLikeRepository, 
            CloudinaryService cloudinaryService, NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.sharePostRepository = sharePostRepository;
        this.mediaRoomRepository = mediaRoomRepository;
        this.mediaRepository = mediaRepository;
        this.postLikeRepository = postLikeRepository;
        this.cloudinaryService = cloudinaryService;
        this.notificationService = notificationService;
    }

    @Override
    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    @Override
    public Optional<Post> getPostById(Long postId) {
        return postRepository.findById(postId);
    }

    @Override
    public Optional<PostItemResponse> getPostItemById(Long postId) {
        return postRepository.findPostItemById(postId);
    }

    @Override
    public List<PostItemResponse> getPublicPosts() {
        return postRepository.findByIsPublicTrue();
    }

    @Override
    public List<PostItemResponse> getPostsByAuthor(User author) {
        return postRepository.findPostItemByAuthor(author);
    }

    @Override
    public List<Media> getPostMedia(Long postId) {
        List<MediaRoom> mediaRooms = mediaRoomRepository.findByTargetIdAndTargetType(
                postId, MediaRoomTargetType.POST);
        mediaRooms.forEach(mediaRoom -> mediaRoom.getMedia().setMediaUrl(cloudinaryService.buildCloudinaryUrl(mediaRoom.getMedia().getMediaUrl())));

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
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new NotFoundException("Media not found with id: " + mediaId));
    
        MediaRoom mediaRoom = MediaRoom.builder()
                .media(media)
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

    @Override
    @Transactional(transactionManager = "transactionManager")
    public boolean toggleLike(Long postId, User user) {
        Post post = getPostById(postId).get();
        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false; // now unliked
        }
        PostLike like = PostLike.builder().post(post).user(user).build();
        postLikeRepository.save(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);
        
        // Gửi notification cho chủ bài viết (trừ khi user like chính bài viết của mình)
        if (!post.getAuthor().getId().equals(user.getId())) {
            notificationService.sendPostLikeNotification(postId, post.getAuthor().getId(), user.getId());
        }
        
        return true; // now liked
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public CommentItemResponse addComment(Long postId, User author, String content, Long parentId) {
        Post post = getPostById(postId).get();
        Comment.CommentBuilder b = Comment.builder().post(post).author(author).content(content);
        if (parentId != null) b.parentComment(commentRepository.findById(parentId).orElseThrow(() -> new NotFoundException("Parent comment not found")));
        Comment saved = commentRepository.save(b.build());
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        
        // Gửi notification cho chủ bài viết (trừ khi user comment chính bài viết của mình)
        if (!post.getAuthor().getId().equals(author.getId())) {
            notificationService.sendPostCommentNotification(postId, post.getAuthor().getId(), author.getId());
        }
        
        return new CommentItemResponse(
            saved.getId(), saved.getContent(), saved.getLikeCount(),
            author.getId(), author.getFirstName(), author.getLastName(), author.getAvatarUrl(),
            saved.getParentComment() != null ? saved.getParentComment().getId() : null,
            post.getId(), saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public SharePostItemResponse sharePost(Long postId, User sharer, String shareContent) {
        Post post = getPostById(postId).get();
        SharePost saved = sharePostRepository.save(
                SharePost.builder().originalPost(post).sharer(sharer).shareContent(shareContent).build());
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
        
        // Gửi notification cho chủ bài viết (trừ khi user share chính bài viết của mình)
        if (!post.getAuthor().getId().equals(sharer.getId())) {
            notificationService.sendPostShareNotification(postId, post.getAuthor().getId(), sharer.getId());
        }
        
        return new SharePostItemResponse(
                saved.getId(), saved.getShareContent(),
                post.getId(),
                sharer.getId(), sharer.getFirstName(), sharer.getLastName(), sharer.getAvatarUrl(),
                saved.getLikeCount(), saved.getCommentCount(), saved.getShareCount(), saved.getViewCount(),
                saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<UserLikeResponse> getUsersLikedPost(Long postId) {
        Post post = getPostById(postId).get();
        List<PostLike> postLikes = postLikeRepository.findAllByPostWithUser(post);
        
        return postLikes.stream()
                .map(PostLike::getUser)
                .map(user -> UserLikeResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .avatarUrl(user.getAvatarUrl())
                    .username(user.getUsername())
                    .roles(user.getRoles().stream()
                        .map(role -> role.name())
                        .collect(Collectors.toSet()))
                    .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<SharePostItemResponse> getSharesOfPost(Long postId) {
        Post post = getPostById(postId).get();
        return sharePostRepository.findByOriginalPost(post).stream()
                .map(sp -> new SharePostItemResponse(
                        sp.getId(), sp.getShareContent(), post.getId(),
                        sp.getSharer().getId(), sp.getSharer().getFirstName(), sp.getSharer().getLastName(), sp.getSharer().getAvatarUrl(),
                        sp.getLikeCount(), sp.getCommentCount(), sp.getShareCount(), sp.getViewCount(), sp.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<CommentItemResponse> getCommentsOfPost(Long postId) {
        Post post = getPostById(postId).get();
        return commentRepository.findTopLevelCommentsByPost(post).stream()
                .map(c -> new CommentItemResponse(
                        c.getId(), c.getContent(), c.getLikeCount(),
                        c.getAuthor().getId(), c.getAuthor().getFirstName(), c.getAuthor().getLastName(), c.getAuthor().getAvatarUrl(),
                        null, post.getId(), c.getCreatedAt()))
                .collect(Collectors.toList());
    }

}