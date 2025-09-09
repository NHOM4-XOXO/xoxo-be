package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.TypingIndicatorRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.TypingIndicatorResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.MessengerChatService;
import com.nhom4.xoxo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/messenger")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messenger", description = "Advanced chat features like Facebook Messenger")
public class MessengerController {

    private final MessengerChatService messengerChatService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findByEmail(email);
    }

    // ==================== Message Status Management ====================

    @Operation(summary = "Mark message as delivered", description = "Mark a specific message as delivered")
    @PostMapping("/messages/{messageId}/delivered")
    public ResponseEntity<WrapRes<Void>> markAsDelivered(@PathVariable String messageId) {
        messengerChatService.markMessageAsDelivered(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Mark message as read", description = "Mark a specific message as read")
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<WrapRes<Void>> markAsRead(@PathVariable String messageId) {
        messengerChatService.markMessageAsRead(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Mark conversation as read", description = "Mark all messages in a conversation as read")
    @PostMapping("/conversations/{chatRoomId}/read")
    public ResponseEntity<WrapRes<Void>> markConversationAsRead(@PathVariable Long chatRoomId) {
        messengerChatService.markConversationAsRead(chatRoomId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    // ==================== Message Reactions ====================

    @Operation(summary = "Add reaction to message", description = "Add emoji reaction to a message")
    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<WrapRes<Void>> addReaction(
            @PathVariable String messageId,
            @RequestParam String reaction) {
        messengerChatService.addReaction(messageId, getCurrentUser().getId(), reaction);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Remove reaction from message", description = "Remove emoji reaction from a message")
    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<WrapRes<Void>> removeReaction(@PathVariable String messageId) {
        messengerChatService.removeReaction(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Get message reactions", description = "Get all reactions for a message")
    @GetMapping("/messages/{messageId}/reactions")
    public ResponseEntity<WrapRes<List<String>>> getMessageReactions(@PathVariable String messageId) {
        List<String> reactions = messengerChatService.getMessageReactions(messageId);
        return ResponseEntity.ok(WrapRes.success(reactions));
    }

    // ==================== Typing Indicators ====================

    @MessageMapping("/typing/start")
    public void startTyping(@Payload TypingIndicatorRequest request, Principal principal) {
        Long userId = Long.parseLong(principal.getName().split("@")[0]); // Extract user ID from email
        messengerChatService.startTyping(request.getChatRoomId(), userId);
    }

    @MessageMapping("/typing/stop")
    public void stopTyping(@Payload TypingIndicatorRequest request, Principal principal) {
        Long userId = Long.parseLong(principal.getName().split("@")[0]);
        messengerChatService.stopTyping(request.getChatRoomId(), userId);
    }

    @Operation(summary = "Get typing users", description = "Get list of users currently typing in a chat room")
    @GetMapping("/conversations/{chatRoomId}/typing")
    public ResponseEntity<WrapRes<TypingIndicatorResponse>> getTypingUsers(@PathVariable Long chatRoomId) {
        TypingIndicatorResponse response = messengerChatService.getTypingUsers(chatRoomId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    // ==================== Message Search ====================

    @Operation(summary = "Search messages in conversation", description = "Search messages within a specific conversation")
    @GetMapping("/conversations/{chatRoomId}/search")
    public ResponseEntity<WrapRes<Page<ChatMessageResponse>>> searchMessages(
            @PathVariable Long chatRoomId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessageResponse> results = messengerChatService.searchMessages(chatRoomId, query, pageable);
        return ResponseEntity.ok(WrapRes.success(results));
    }

    @Operation(summary = "Global message search", description = "Search messages across all user's conversations")
    @GetMapping("/search")
    public ResponseEntity<WrapRes<Page<ChatMessageResponse>>> searchGlobalMessages(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessageResponse> results = messengerChatService.searchGlobalMessages(getCurrentUser().getId(), query, pageable);
        return ResponseEntity.ok(WrapRes.success(results));
    }

    // ==================== Message Management ====================

    @Operation(summary = "Edit message", description = "Edit an existing message (within 24 hours)")
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<WrapRes<ChatMessageResponse>> editMessage(
            @PathVariable String messageId,
            @RequestParam String content) {
        
        ChatMessageResponse response = messengerChatService.editMessage(messageId, content, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @Operation(summary = "Delete message", description = "Delete a message (soft delete)")
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<WrapRes<Void>> deleteMessage(@PathVariable String messageId) {
        messengerChatService.deleteMessage(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Pin message", description = "Pin an important message in conversation")
    @PostMapping("/messages/{messageId}/pin")
    public ResponseEntity<WrapRes<Void>> pinMessage(@PathVariable String messageId) {
        messengerChatService.pinMessage(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Unpin message", description = "Unpin a message from conversation")
    @DeleteMapping("/messages/{messageId}/pin")
    public ResponseEntity<WrapRes<Void>> unpinMessage(@PathVariable String messageId) {
        messengerChatService.unpinMessage(messageId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Get pinned messages", description = "Get all pinned messages in a conversation")
    @GetMapping("/conversations/{chatRoomId}/pinned")
    public ResponseEntity<WrapRes<List<ChatMessageResponse>>> getPinnedMessages(@PathVariable Long chatRoomId) {
        List<ChatMessageResponse> pinnedMessages = messengerChatService.getPinnedMessages(chatRoomId);
        return ResponseEntity.ok(WrapRes.success(pinnedMessages));
    }

    // ==================== Analytics ====================

    @Operation(summary = "Get unread message count", description = "Get total unread message count for user")
    @GetMapping("/unread-count")
    public ResponseEntity<WrapRes<Map<String, Integer>>> getUnreadMessageCount() {
        int count = messengerChatService.getUnreadMessageCount(getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(Map.of("unreadCount", count)));
    }

    @Operation(summary = "Get unread count for conversation", description = "Get unread message count for specific conversation")
    @GetMapping("/conversations/{chatRoomId}/unread-count")
    public ResponseEntity<WrapRes<Map<String, Integer>>> getUnreadMessageCountForRoom(@PathVariable Long chatRoomId) {
        int count = messengerChatService.getUnreadMessageCountForRoom(chatRoomId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(Map.of("unreadCount", count)));
    }

    // ==================== Bulk Operations ====================

    @Operation(summary = "Mark multiple messages as read", description = "Mark multiple messages as read in bulk")
    @PostMapping("/messages/bulk/read")
    public ResponseEntity<WrapRes<Void>> markMultipleAsRead(@RequestBody List<String> messageIds) {
        messengerChatService.markMultipleMessagesAsRead(messageIds, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @Operation(summary = "Delete multiple messages", description = "Delete multiple messages in bulk")
    @DeleteMapping("/messages/bulk")
    public ResponseEntity<WrapRes<Void>> deleteMultipleMessages(@RequestBody List<String> messageIds) {
        messengerChatService.deleteMultipleMessages(messageIds, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }
}




