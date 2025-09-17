package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.req.TypingIndicatorRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.TypingIndicatorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessengerChatService {
    
    // Message Status Management
    void markMessageAsDelivered(String messageId, Long userId);
    void markMessageAsRead(String messageId, Long userId);
    void markConversationAsRead(Long chatRoomId, Long userId);
    
    // Message Reactions
    void addReaction(String messageId, Long userId, String reaction);
    void removeReaction(String messageId, Long userId);
    List<String> getMessageReactions(String messageId);
    
    // Typing Indicators
    void startTyping(Long chatRoomId, Long userId);
    void stopTyping(Long chatRoomId, Long userId);
    TypingIndicatorResponse getTypingUsers(Long chatRoomId);
    
    // Message Search
    Page<ChatMessageResponse> searchMessages(Long chatRoomId, String query, Pageable pageable);
    Page<ChatMessageResponse> searchGlobalMessages(Long userId, String query, Pageable pageable);
    
    // Message Management
    ChatMessageResponse editMessage(String messageId, String newContent, Long userId);
    void deleteMessage(String messageId, Long userId);
    ChatMessageResponse forwardMessage(String messageId, Long targetChatRoomId, Long userId);
    void pinMessage(String messageId, Long userId);
    void unpinMessage(String messageId, Long userId);
    List<ChatMessageResponse> getPinnedMessages(Long chatRoomId);
    
    // Advanced Features
    ChatMessageResponse replyToMessage(String originalMessageId, String replyContent, Long userId);
    void createMessageThread(String messageId);
    Page<ChatMessageResponse> getMessageThread(String threadId, Pageable pageable);
    
    // Online Status
    void updateUserOnlineStatus(Long userId, boolean isOnline);
    void updateLastSeen(Long userId);
    List<Long> getOnlineUsers(Long chatRoomId);
    
    // Message Analytics
    int getUnreadMessageCount(Long userId);
    int getUnreadMessageCountForRoom(Long chatRoomId, Long userId);
    
    // Bulk Operations
    void markMultipleMessagesAsRead(List<String> messageIds, Long userId);
    void deleteMultipleMessages(List<String> messageIds, Long userId);
}












