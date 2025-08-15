package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Post;
import com.nhom4.xoxo.entity.PostLike;
import com.nhom4.xoxo.entity.User;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post = :post")
    long countByPost(@Param("post") Post post);

    @Query("SELECT pl FROM PostLike pl WHERE pl.post = :post AND pl.user = :user")
    Optional<PostLike> findByPostAndUser(@Param("post") Post post, @Param("user") User user);

    @Query("SELECT pl FROM PostLike pl JOIN FETCH pl.user u WHERE pl.post = :post")
    List<PostLike> findAllByPostWithUser(@Param("post") Post post);
}


