package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.ChatMessage;
import com.nhom4.xoxo.entity.ChatRoom;
import com.nhom4.xoxo.enums.ChatRoomType;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // Thêm query này vào repository
    @EntityGraph("ChatRoom.withMessages")
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId AND cr.active = true ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findChatRoomsByUserIdWithMessages(@Param("userId") Long userId);

    // Thêm query để lấy last message cho mỗi chat room
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId AND m.deleted = false ORDER BY m.sentAt DESC LIMIT 1")
    Optional<ChatMessage> findLastMessageByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.id = :userId AND cr.active = true ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = :type AND cr.active = true")
    List<ChatRoom> findChatRoomsByType(@Param("type") ChatRoomType type);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p1 JOIN cr.participants p2 " +
            "WHERE p1.id = :userId1 AND p2.id = :userId2 AND cr.type = 'DIRECT' AND cr.active = true")
    Optional<ChatRoom> findDirectChatRoom(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.createdBy = :userId AND cr.active = true")
    List<ChatRoom> findChatRoomsCreatedByUser(@Param("userId") Long userId);
}
