package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.deleted = false ORDER BY cm.sentAt DESC")
    Page<ChatMessage> findMessagesByChatRoomId(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.sender.id = :senderId AND cm.deleted = false ORDER BY cm.sentAt DESC")
    List<ChatMessage> findMessagesByChatRoomAndSender(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.read = false AND cm.sender.id != :userId")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.deleted = false ORDER BY cm.sentAt ASC")
    List<ChatMessage> findAllMessagesByChatRoomId(@Param("chatRoomId") Long chatRoomId);
}
