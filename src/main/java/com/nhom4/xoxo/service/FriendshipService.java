package com.nhom4.xoxo.service;

import java.util.List;

import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.entity.User;

public interface FriendshipService {

    // Tạo lời mời kết bạn
    Friendship createFriendship(Long userId, Long friendId);

    // Hủy kết bạn
    Friendship cancelFriendship(Long friendshipId, Long userId);

    // Hủy lời mời kết bạn
    Friendship cancelFriendshipRequest(Long friendshipId, Long userId);
    
    // Xác nhận lời mời kết bạn
    Friendship acceptFriendship(Long friendshipId, Long userId);
    
    // Từ chối lời mời kết bạn
    Friendship rejectFriendship(Long friendshipId, Long userId);

    // Tìm tất cả bạn bè của 1 user
    List<User> getFriendsByUserId(Long userId);
    
    // Tìm tất cả lời mời kết bạn đang chờ xác nhận
    List<Friendship> getPendingFriendships(Long userId);
    
    // Tìm tất cả lời mời kết bạn đã gửi
    List<Friendship> getSentFriendships(Long userId);

    // Kiểm tra 2 user có phải bạn bè không
    boolean areFriends(Long userId1, Long userId2);

    // Kiểm tra có lời mời kết bạn nào giữa 2 user không
    boolean hasPendingRequest(Long userId1, Long userId2);
}
