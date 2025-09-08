package com.nhom4.xoxo.chat;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoChatMessageRepository extends MongoRepository<MongoChatMessage, String> {
    
    // Basic queries
    @Query("{'chatRoomId': ?0, 'deleted': {$ne: true}}")
    Page<MongoChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);
    
    @Query("{'chatRoomId': ?0, 'senderId': ?1, 'deleted': {$ne: true}}")
    List<MongoChatMessage> findByChatRoomIdAndSenderIdOrderBySentAtDesc(Long chatRoomId, Long senderId);
    
    @Query("{'chatRoomId': ?0, 'readBy.?1': {$exists: false}, 'senderId': {$ne: ?1}, 'deleted': {$ne: true}}")
    List<MongoChatMessage> findUnreadMessagesByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    
    @Query("{'chatRoomId': ?0, 'deleted': {$ne: true}}")
    List<MongoChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
    
    @Query(value = "{'chatRoomId': ?0, 'deleted': {$ne: true}}", count = true)
    long countByChatRoomId(Long chatRoomId);
    
    // Messenger-like features
    @Query("{'chatRoomId': ?0, 'pinned': true, 'deleted': {$ne: true}}")
    List<MongoChatMessage> findPinnedMessagesByChatRoomId(Long chatRoomId);
    
    @Query("{'chatRoomId': ?0, 'important': true, 'deleted': {$ne: true}}")
    List<MongoChatMessage> findImportantMessagesByChatRoomId(Long chatRoomId);
    
    // Search with text index
    @Query("{'chatRoomId': ?0, '$text': {'$search': ?1}, 'deleted': {$ne: true}}")
    Page<MongoChatMessage> findByChatRoomIdAndTextSearch(Long chatRoomId, String searchText, Pageable pageable);
    
    @Query("{'$text': {'$search': ?0}, 'deleted': {$ne: true}}")
    Page<MongoChatMessage> findByTextSearch(String searchText, Pageable pageable);
    
    // Analytics queries
    @Query(value = "{'chatRoomId': ?0, 'sentAt': {$gte: ?1, $lte: ?2}, 'deleted': {$ne: true}}", count = true)
    long countMessagesByChatRoomIdAndDateRange(Long chatRoomId, Instant startDate, Instant endDate);
    
    @Query("{'chatRoomId': ?0, 'type': ?1, 'deleted': {$ne: true}}")
    long countByChatRoomIdAndType(Long chatRoomId, String messageType);
    
    @Query("{'chatRoomId': ?0, 'reactions': {$exists: true, $ne: {}}}")
    long countMessagesWithReactions(Long chatRoomId);
    
    // User activity
    @Query("{'chatRoomId': ?0, 'senderId': ?1, 'sentAt': {$gte: ?2}, 'deleted': {$ne: true}}")
    long countByUserAndDateRange(Long chatRoomId, Long userId, Instant startDate);
    
    // Read receipts
    @Query("{'readBy.?0': {$exists: false}, 'senderId': {$ne: ?0}, 'deleted': {$ne: true}}")
    long countUnreadMessagesByUser(Long userId);
    
    @Query("{'chatRoomId': ?0, 'readBy.?1': {$exists: false}, 'senderId': {$ne: ?1}, 'deleted': {$ne: true}}")
    long countUnreadMessagesByUserAndRoom(Long chatRoomId, Long userId);
    
    // Media statistics
    @Query("{'chatRoomId': ?0, 'type': {$in: ['IMAGE', 'VIDEO', 'FILE']}, 'deleted': {$ne: true}}")
    long countMediaMessages(Long chatRoomId);
    
    // Thread messages
    @Query("{'threadId': ?0, 'deleted': {$ne: true}}")
    Page<MongoChatMessage> findByThreadId(String threadId, Pageable pageable);
    
    // Performance optimized queries
    @Query(value = "{'chatRoomId': ?0, 'deleted': {$ne: true}}", 
           fields = "{'id': 1, 'content': 1, 'senderId': 1, 'sentAt': 1, 'senderName': 1}")
    List<MongoChatMessage> findBasicMessagesByChatRoomId(Long chatRoomId);
    
    // Recent activity
    @Query("{'chatRoomId': ?0, 'sentAt': {$gte: ?1}, 'deleted': {$ne: true}}")
    long countRecentMessages(Long chatRoomId, Instant since);
    
    // Message with reactions
    @Query("{'chatRoomId': ?0, 'reactions.?1': {$gt: 0}}")
    List<MongoChatMessage> findMessagesWithSpecificReaction(Long chatRoomId, String reaction);
}
