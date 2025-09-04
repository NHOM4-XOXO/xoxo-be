// package com.nhom4.xoxo.controller;

// import java.util.List;

// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.PutMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.multipart.MultipartFile;

// import com.nhom4.xoxo.dto.WrapRes;
// import com.nhom4.xoxo.dto.req.ChatMessageRequest;
// import com.nhom4.xoxo.dto.req.CreateChatRoomRequest;
// import com.nhom4.xoxo.dto.req.PushNotificationRequest;
// import com.nhom4.xoxo.dto.res.ChatMessageResponse;
// import com.nhom4.xoxo.dto.res.ChatRoomResponse;
// import com.nhom4.xoxo.dto.res.FileUploadResponse;
// import com.nhom4.xoxo.entity.User;
// import com.nhom4.xoxo.repository.UserRepository;
// import com.nhom4.xoxo.service.EnhancedChatService;

// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @RestController
// @RequestMapping("/api/v1/enhanced-chat")
// @RequiredArgsConstructor
// @Slf4j
// @Tag(name = "Enhanced Chat", description = "Advanced chat features including encryption, file sharing, and push notifications")
// public class EnhancedChatController {

//     private final EnhancedChatService enhancedChatService;
//     private final UserRepository userRepository;
//     // End-to-End Encryption Endpoints
//     @PostMapping("/encryption/keys/generate")
//     @Operation(summary = "Generate encryption keys for current user")
//     public ResponseEntity<WrapRes<Void>> generateEncryptionKeys() {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.generateUserEncryptionKeys(currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @GetMapping("/encryption/keys/public/{userId}")
//     @Operation(summary = "Get user's public encryption key")
//     public ResponseEntity<WrapRes<String>> getUserPublicKey(@PathVariable Long userId) {
//         String publicKey = enhancedChatService.getUserPublicKey(userId);
//         return ResponseEntity.ok(WrapRes.success(publicKey));
//     }

//     @PostMapping("/messages/encrypted")
//     @Operation(summary = "Send encrypted message")
//     public ResponseEntity<WrapRes<ChatMessageResponse>> sendEncryptedMessage(@RequestBody ChatMessageRequest request) {
//         Long currentUserId = getCurrentUserId();
//         ChatMessageResponse response = enhancedChatService.sendEncryptedMessage(request, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(response));
//     }

//     @GetMapping("/messages/{messageId}/decrypt")
//     @Operation(summary = "Decrypt message for current user")
//     public ResponseEntity<WrapRes<String>> decryptMessage(@PathVariable Long messageId) {
//         Long currentUserId = getCurrentUserId();
//         String decryptedMessage = enhancedChatService.decryptMessageForUser(messageId, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(decryptedMessage));
//     }

//     // File Sharing Endpoints
//     @PostMapping("/rooms/{chatRoomId}/files/upload")
//     @Operation(summary = "Upload file to chat room")
//     public ResponseEntity<WrapRes<FileUploadResponse>> uploadFile(
//             @PathVariable Long chatRoomId,
//             @RequestParam("file") MultipartFile file) {
//         Long currentUserId = getCurrentUserId();
//         FileUploadResponse response = enhancedChatService.uploadFileToChat(chatRoomId, file, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(response));
//     }

//     @GetMapping("/rooms/{chatRoomId}/files")
//     @Operation(summary = "Get files in chat room")
//     public ResponseEntity<WrapRes<Page<FileUploadResponse>>> getChatFiles(
//             @PathVariable Long chatRoomId,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "20") int size) {
//         Long currentUserId = getCurrentUserId();
//         Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
//         Page<FileUploadResponse> response = enhancedChatService.getChatFiles(chatRoomId, currentUserId, pageable);
//         return ResponseEntity.ok(WrapRes.success(response));
//     }

//     @GetMapping("/files/{fileId}/download")
//     @Operation(summary = "Download file")
//     public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
//         Long currentUserId = getCurrentUserId();
//         byte[] fileData = enhancedChatService.downloadFile(fileId, currentUserId);
        
//         // Get file info for headers
//         // This would need to be implemented to get file metadata
//         String fileName = "file"; // Default name
        
//         HttpHeaders headers = new HttpHeaders();
//         headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//         headers.setContentDispositionFormData("attachment", fileName);
        
//         return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
//     }

//     @DeleteMapping("/files/{fileId}")
//     @Operation(summary = "Delete file")
//     public ResponseEntity<WrapRes<Void>> deleteFile(@PathVariable Long fileId) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.deleteFile(fileId, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @PostMapping("/files/{fileId}/share")
//     @Operation(summary = "Share file with specific users")
//     public ResponseEntity<WrapRes<Void>> shareFile(
//             @PathVariable Long fileId,
//             @RequestBody List<Long> userIds) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.shareFileWithUsers(fileId, userIds, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     // Push Notification Endpoints
//     @PostMapping("/devices/register")
//     @Operation(summary = "Register user device for push notifications")
//     public ResponseEntity<WrapRes<Void>> registerDevice(
//             @RequestParam String deviceId,
//             @RequestParam String fcmToken,
//             @RequestParam String deviceType,
//             @RequestParam String deviceModel,
//             @RequestParam String operatingSystem,
//             @RequestParam String appVersion) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.updateUserDevice(currentUserId, deviceId, fcmToken, deviceType, 
//                                           deviceModel, operatingSystem, appVersion);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @PostMapping("/notifications/toggle")
//     @Operation(summary = "Enable/disable push notifications")
//     public ResponseEntity<WrapRes<Void>> togglePushNotifications(@RequestParam boolean enabled) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.togglePushNotifications(currentUserId, enabled);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     // Advanced Features
//     @GetMapping("/rooms/{chatRoomId}/messages/search")
//     @Operation(summary = "Search messages in chat room")
//     public ResponseEntity<WrapRes<Page<ChatMessageResponse>>> searchMessages(
//             @PathVariable Long chatRoomId,
//             @RequestParam String query,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "20") int size) {
//         Long currentUserId = getCurrentUserId();
//         Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
//         Page<ChatMessageResponse> response = enhancedChatService.searchMessages(chatRoomId, query, currentUserId, pageable);
//         return ResponseEntity.ok(WrapRes.success(response));
//     }

//     @PostMapping("/messages/{messageId}/pin")
//     @Operation(summary = "Pin message in chat room")
//     public ResponseEntity<WrapRes<Void>> pinMessage(@PathVariable Long messageId) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.pinMessage(messageId, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @DeleteMapping("/messages/{messageId}/pin")
//     @Operation(summary = "Unpin message in chat room")
//     public ResponseEntity<WrapRes<Void>> unpinMessage(@PathVariable Long messageId) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.unpinMessage(messageId, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @GetMapping("/rooms/{chatRoomId}/messages/pinned")
//     @Operation(summary = "Get pinned messages in chat room")
//     public ResponseEntity<WrapRes<List<ChatMessageResponse>>> getPinnedMessages(@PathVariable Long chatRoomId) {
//         Long currentUserId = getCurrentUserId();
//         List<ChatMessageResponse> response = enhancedChatService.getPinnedMessages(chatRoomId, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(response));
//     }

//     @PostMapping("/messages/{messageId}/reactions")
//     @Operation(summary = "React to message")
//     public ResponseEntity<WrapRes<Void>> reactToMessage(
//             @PathVariable Long messageId,
//             @RequestParam String reaction) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.reactToMessage(messageId, reaction, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     @DeleteMapping("/messages/{messageId}/reactions/{reaction}")
//     @Operation(summary = "Remove reaction from message")
//     public ResponseEntity<WrapRes<Void>> removeReaction(
//             @PathVariable Long messageId,
//             @PathVariable String reaction) {
//         Long currentUserId = getCurrentUserId();
//         enhancedChatService.removeReaction(messageId, reaction, currentUserId);
//         return ResponseEntity.ok(WrapRes.success(null));
//     }

//     private Long getCurrentUserId() {
//         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//         if (authentication == null || !authentication.isAuthenticated()) {
//             throw new RuntimeException("User not authenticated");
//         }
//         String usernameOrEmail = authentication.getName(); // lấy từ JWT
//         return userRepository.findByUsername(usernameOrEmail)
//             .or(() -> userRepository.findByEmail(usernameOrEmail))
//             .map(User::getId)
//             .orElseThrow(() -> new RuntimeException("User not found: " + usernameOrEmail));
//     }
// }
