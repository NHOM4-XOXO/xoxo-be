package com.nhom4.xoxo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nhom4.xoxo.dto.req.ChatMessageRequest;
import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.ChatRoomResponse;

public interface ChatService {
    
    // Chat Room Management
    ChatRoomResponse createChatRoom(CreateChatRoomRequest request, Long currentUserId);
    ChatRoomResponse getChatRoomById(Long chatRoomId, Long currentUserId);
    List<ChatRoomResponse> getUserChatRooms(Long userId);
    ChatRoomResponse updateChatRoom(Long chatRoomId, CreateChatRoomRequest request, Long currentUserId);
    void deleteChatRoom(Long chatRoomId, Long currentUserId);
    
    // Chat Message Management
    ChatMessageResponse sendMessage(ChatMessageRequest request, Long currentUserId);
    Page<ChatMessageResponse> getChatMessages(Long chatRoomId, Long currentUserId, Pageable pageable);
    ChatMessageResponse getMessageById(Long messageId, Long currentUserId);
    void deleteMessage(Long messageId, Long currentUserId);
    
    // Participant Management
    void addParticipant(Long chatRoomId, Long userId, Long currentUserId);
    void removeParticipant(Long chatRoomId, Long userId, Long currentUserId);
    void leaveChatRoom(Long chatRoomId, Long currentUserId);
    
    // Real-time Chat
    void markMessageAsRead(Long messageId, Long currentUserId);
    void markMessageAsDelivered(Long messageId, Long currentUserId);
    Long getUnreadMessageCount(Long chatRoomId, Long currentUserId);
    
    // Direct Chat
    ChatRoomResponse getOrCreateDirectChat(Long otherUserId, Long currentUserId);
}
