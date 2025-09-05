package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.dto.res.PostItemResponse;
import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
            select new com.nhom4.xoxo.dto.res.PostItemResponse(
              p.id,
              p.content,
              p.status,
              p.type,
              p.location,
              p.hashtags,
              p.isPublic,
              p.allowComments,
              p.allowLikes,
              p.allowShares,
              p.likeCount,
              p.commentCount,
              p.shareCount,
              p.viewCount,
              a.id,
              a.firstName,
              a.lastName,
              a.avatarUrl,
              p.createdAt,
              p.updatedAt
            )
            from Post p join p.author a
            where p.id = :postId
            """)
    Optional<PostItemResponse> findPostItemById(@Param("postId") Long postId);

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

    // Tìm posts public (projection kèm authorId/authorUsername/authorAvatarUrl)
    @Query("""
            select new com.nhom4.xoxo.dto.res.PostItemResponse(
              p.id,
              p.content,
              p.status,
              p.type,
              p.location,
              p.hashtags,
              p.isPublic,
              p.allowComments,
              p.allowLikes,
              p.allowShares,
              p.likeCount,
              p.commentCount,
              p.shareCount,
              p.viewCount,
              a.id,
              a.firstName,
              a.lastName,
              a.avatarUrl,
              p.createdAt,
              p.updatedAt
            )
            from Post p join p.author a
            where p.isPublic = true and p.status = 'ACTIVE'
            order by p.createdAt desc
            """)
    List<PostItemResponse> findByIsPublicTrue();
    
    @Query("""
        select new com.nhom4.xoxo.dto.res.PostItemResponse(
          p.id,
          p.content,
          p.status,
          p.type,
          p.location,
          p.hashtags,
          p.isPublic,
          p.allowComments,
          p.allowLikes,
          p.allowShares,
          p.likeCount,
          p.commentCount,
          p.shareCount,
          p.viewCount,
          a.id,
          a.firstName,
          a.lastName,
          a.avatarUrl,
          p.createdAt,
          p.updatedAt
        )
        from Post p join p.author a
        order by p.createdAt desc
        """)
    List<PostItemResponse> findAll2();
    // Tìm posts theo author và public
    @Query("""
            select new com.nhom4.xoxo.dto.res.PostItemResponse(
              p.id,
              p.content,
              p.status,
              p.type,       
              p.location,
              p.hashtags,
              p.isPublic,
              p.allowComments,
              p.allowLikes,
              p.allowShares,
              p.likeCount,
              p.commentCount,
              p.shareCount,
              p.viewCount,
              a.id,
              a.firstName,
              a.lastName,
              a.avatarUrl,
              p.createdAt,  
              p.updatedAt
            )
            from Post p join p.author a
            where p.author = :author and p.isPublic = true
            order by p.createdAt desc
            """)    
    List<PostItemResponse> findPostItemByAuthor(@Param("author") User author);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
    Post findByIdWithAuthor(@Param("id") Long id);
    
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

    // Lấy danh sách Media của bài viết qua bảng media_room, fetch uploadedBy để tránh lazy issues
    @Query("SELECT m FROM MediaRoom mr JOIN mr.media m LEFT JOIN FETCH m.uploadedBy WHERE mr.targetId = :postId AND mr.targetType = com.nhom4.xoxo.enums.MediaRoomTargetType.POST")
    List<Media> findMediaByPostId(@Param("postId") Long postId);
}