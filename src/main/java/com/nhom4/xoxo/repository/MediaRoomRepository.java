package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Media;
import com.nhom4.xoxo.entity.MediaRoom;
import com.nhom4.xoxo.enums.MediaRoomTargetType;

@Repository
public interface MediaRoomRepository extends JpaRepository<MediaRoom, Long> {
    
    @Query("SELECT mr FROM MediaRoom mr WHERE mr.targetId = :targetId AND mr.targetType = :targetType")
    List<MediaRoom> findByTargetIdAndTargetType(@Param("targetId") Long targetId, @Param("targetType") MediaRoomTargetType targetType);
    
    @Query("SELECT mr FROM MediaRoom mr WHERE mr.targetId = :targetId")
    List<MediaRoom> findByTargetId(@Param("targetId") Long targetId);
    
    @Query("SELECT mr FROM MediaRoom mr WHERE mr.media.id = :mediaId")
    List<MediaRoom> findByMediaId(@Param("mediaId") Long mediaId);
    
    @Query("SELECT mr FROM MediaRoom mr WHERE mr.targetType = :targetType")
    List<MediaRoom> findByTargetType(@Param("targetType") MediaRoomTargetType targetType);
    
    // Tìm theo Media entity
    List<MediaRoom> findByMedia(Media media);
} 