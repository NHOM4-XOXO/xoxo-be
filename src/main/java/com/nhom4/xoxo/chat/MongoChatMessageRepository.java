package com.nhom4.xoxo.chat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoChatMessageRepository extends MongoRepository<MongoChatMessage, String> {
    
    @Query("{'chatRoomId': ?0, 'deleted': false}")
    Page<MongoChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);
    
    @Query("{'chatRoomId': ?0, 'senderId': ?1, 'deleted': false}")
    List<MongoChatMessage> findByChatRoomIdAndSenderIdOrderBySentAtDesc(Long chatRoomId, Long senderId);
    
    @Query("{'chatRoomId': ?0, 'read': false, 'senderId': {$ne: ?1}, 'deleted': false}")
    List<MongoChatMessage> findUnreadMessagesByChatRoomIdAndUserId(Long chatRoomId, Long userId);
    
    @Query("{'chatRoomId': ?0, 'deleted': false}")
    List<MongoChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
    
    @Query(value = "{'chatRoomId': ?0, 'deleted': false}", count = true)
    long countByChatRoomId(Long chatRoomId);
}
