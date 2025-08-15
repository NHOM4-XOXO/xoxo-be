package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.ChatParticipant;
import com.nhom4.xoxo.enums.ParticipantStatus;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.chatRoom.id = :chatRoomId AND cp.status = :status")
    List<ChatParticipant> findParticipantsByChatRoomAndStatus(@Param("chatRoomId") Long chatRoomId, @Param("status") ParticipantStatus status);
    
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.user.id = :userId AND cp.chatRoom.id = :chatRoomId")
    Optional<ChatParticipant> findByUserAndChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);
    
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.user.id = :userId AND cp.active = true")
    List<ChatParticipant> findActiveChatsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.chatRoom.id = :chatRoomId AND cp.active = true")
    List<ChatParticipant> findActiveParticipantsByChatRoom(@Param("chatRoomId") Long chatRoomId);
}
