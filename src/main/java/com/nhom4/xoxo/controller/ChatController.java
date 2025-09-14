package com.nhom4.xoxo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom4.xoxo.dto.req.ChatMessageRequest;
import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
import com.nhom4.xoxo.dto.res.ChatMessageResponse;
import com.nhom4.xoxo.dto.res.ChatRoomResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat management APIs")
public class ChatController {

    private final UserRepository userRepository;

    private final ChatService chatService;

    // WebSocket Message Handlers
    @MessageMapping("/send-message")
    public ChatMessageResponse handleChatMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        log.info("Received chat message from user {} in room {}", currentUserId, request.getChatRoomId());
        return chatService.sendMessage(request, currentUserId);
    }

    @MessageMapping("/private-message")
    @SendToUser("/queue/private-message")
    public ChatMessageResponse handlePrivateMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        log.info("Received private message from user {} to room {}", currentUserId, request.getChatRoomId());
        return chatService.sendMessage(request, currentUserId);
    }

    // REST API Endpoints
    @PostMapping("/rooms")
    @Operation(summary = "Create a new chat room")
    public ResponseEntity<WrapRes<ChatRoomResponse>> createChatRoom(@RequestBody CreateChatRoomRequest request,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        ChatRoomResponse response = chatService.createChatRoom(request, currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get user's chat rooms")
    public ResponseEntity<WrapRes<List<ChatRoomResponse>>> getUserChatRooms(Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        List<ChatRoomResponse> response = chatService.getUserChatRooms(currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/rooms/{chatRoomId}")
    @Operation(summary = "Get chat room by ID")
    public ResponseEntity<WrapRes<ChatRoomResponse>> getChatRoom(@PathVariable Long chatRoomId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        ChatRoomResponse response = chatService.getChatRoomById(chatRoomId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @PutMapping("/rooms/{chatRoomId}")
    @Operation(summary = "Update chat room")
    public ResponseEntity<WrapRes<ChatRoomResponse>> updateChatRoom(
            @PathVariable Long chatRoomId,
            @RequestBody CreateChatRoomRequest request,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        ChatRoomResponse response = chatService.updateChatRoom(chatRoomId, request, currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @DeleteMapping("/rooms/{chatRoomId}")
    @Operation(summary = "Delete chat room")
    public ResponseEntity<WrapRes<Void>> deleteChatRoom(@PathVariable Long chatRoomId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.deleteChatRoom(chatRoomId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @PostMapping("/rooms/{chatRoomId}/participants/{userId}")
    @Operation(summary = "Add participant to chat room")
    public ResponseEntity<WrapRes<Void>> addParticipant(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.addParticipant(chatRoomId, userId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @DeleteMapping("/rooms/{chatRoomId}/participants/{userId}")
    @Operation(summary = "Remove participant from chat room")
    public ResponseEntity<WrapRes<Void>> removeParticipant(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.removeParticipant(chatRoomId, userId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @PostMapping("/rooms/{chatRoomId}/leave")
    @Operation(summary = "Leave chat room")
    public ResponseEntity<WrapRes<Void>> leaveChatRoom(@PathVariable Long chatRoomId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.leaveChatRoom(chatRoomId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @GetMapping("/rooms/{chatRoomId}/messages")
    @Operation(summary = "Get chat messages")
    public ResponseEntity<WrapRes<Page<ChatMessageResponse>>> getChatMessages(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<ChatMessageResponse> response = chatService.getChatMessages(chatRoomId, currentUserId, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/messages/{messageId}")
    @Operation(summary = "Get message by ID")
    public ResponseEntity<WrapRes<ChatMessageResponse>> getMessage(@PathVariable Long messageId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        ChatMessageResponse response = chatService.getMessageById(messageId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete message")
    public ResponseEntity<WrapRes<Void>> deleteMessage(@PathVariable Long messageId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.deleteMessage(messageId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @PostMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message as read")
    public ResponseEntity<WrapRes<Void>> markMessageAsRead(@PathVariable Long messageId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.markMessageAsRead(messageId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @PostMapping("/messages/{messageId}/delivered")
    @Operation(summary = "Mark message as delivered")
    public ResponseEntity<WrapRes<Void>> markMessageAsDelivered(@PathVariable Long messageId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        chatService.markMessageAsDelivered(messageId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @GetMapping("/rooms/{chatRoomId}/unread-count")
    @Operation(summary = "Get unread message count")
    public ResponseEntity<WrapRes<Long>> getUnreadMessageCount(@PathVariable Long chatRoomId, Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        Long count = chatService.getUnreadMessageCount(chatRoomId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(count));
    }

    @PostMapping("/direct/{otherUserId}")
    @Operation(summary = "Get or create direct chat with user")
    public ResponseEntity<WrapRes<ChatRoomResponse>> getOrCreateDirectChat(@PathVariable Long otherUserId,
            Principal principal) {
        Long currentUserId = getCurrentUserId(principal);
        ChatRoomResponse response = chatService.getOrCreateDirectChat(otherUserId, currentUserId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    private Long getCurrentUserId(Principal principal) {
        String username = principal.getName();
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
