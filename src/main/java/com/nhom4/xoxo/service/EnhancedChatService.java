package com.nhom4.xoxo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.nhom4.xoxo.dto.req.ChatMessageRequest;
import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
import com.nhom4.xoxo.dto.req.PushNotificationRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.ChatRoomResponse;
import com.nhom4.xoxo.dto.res.FileUploadResponse;

public interface EnhancedChatService extends ChatService {
    
    // End-to-End Encryption
    /**
     * Generate encryption keys for user
     */
    void generateUserEncryptionKeys(Long userId);
    
    /**
     * Get user's public key for encryption
     */
    String getUserPublicKey(Long userId);
    
    /**
     * Send encrypted message
     */
    ChatMessageResponse sendEncryptedMessage(ChatMessageRequest request, Long currentUserId);
    
    /**
     * Decrypt message for user
     */
    String decryptMessageForUser(Long messageId, Long userId);
    
    // File Sharing
    /**
     * Upload file to chat
     */
    FileUploadResponse uploadFileToChat(Long chatRoomId, MultipartFile file, Long currentUserId);
    
    /**
     * Get files in chat room
     */
    Page<FileUploadResponse> getChatFiles(Long chatRoomId, Long currentUserId, Pageable pageable);
    
    /**
     * Download file
     */
    byte[] downloadFile(Long fileId, Long currentUserId);
    
    /**
     * Delete file
     */
    void deleteFile(Long fileId, Long currentUserId);
    
    /**
     * Share file with specific users
     */
    void shareFileWithUsers(Long fileId, List<Long> userIds, Long currentUserId);
    
    // Push Notifications
    /**
     * Send push notification for new message
     */
    void sendMessageNotification(Long chatRoomId, ChatMessageResponse message, List<Long> recipientIds);
    
    /**
     * Send typing indicator notification
     */
    void sendTypingNotification(Long chatRoomId, Long userId, boolean isTyping);
    
    /**
     * Send user online/offline notification
     */
    void sendUserStatusNotification(Long userId, boolean isOnline);
    
    /**
     * Update user device information
     */
    void updateUserDevice(Long userId, String deviceId, String fcmToken, String deviceType, 
                         String deviceModel, String operatingSystem, String appVersion);
    
    /**
     * Enable/disable push notifications for user
     */
    void togglePushNotifications(Long userId, boolean enabled);
    
    // Advanced Features
    /**
     * Search messages in chat room
     */
    Page<ChatMessageResponse> searchMessages(Long chatRoomId, String query, Long currentUserId, Pageable pageable);
    
    /**
     * Pin message in chat room
     */
    void pinMessage(Long messageId, Long currentUserId);
    
    /**
     * Unpin message in chat room
     */
    void unpinMessage(Long messageId, Long currentUserId);
    
    /**
     * Get pinned messages in chat room
     */
    List<ChatMessageResponse> getPinnedMessages(Long chatRoomId, Long currentUserId);
    
    /**
     * React to message
     */
    void reactToMessage(Long messageId, String reaction, Long currentUserId);
    
    /**
     * Remove reaction from message
     */
    void removeReaction(Long messageId, String reaction, Long currentUserId);
}
