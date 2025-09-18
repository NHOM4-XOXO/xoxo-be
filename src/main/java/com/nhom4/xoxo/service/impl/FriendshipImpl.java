package com.nhom4.xoxo.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.FriendshipStatus;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.repository.FriendshipRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.FriendshipService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipImpl implements FriendshipService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    public Friendship createFriendship(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));
        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ServiceException("Friend not found"));
        
        // Kiểm tra không cho gửi lời mời kết bạn với admin/owner
        boolean isFriendAdminOrOwner = friend.getRoles().stream()
                .anyMatch(role -> role == Role.ADMIN || role == Role.OWNER);
        
        if (isFriendAdminOrOwner) {
            throw new NotFoundException("Không thể gửi lời mời kết bạn với người dùng này");
        }
        
        // Kiểm tra không tự kết bạn với chính mình
        if (userId.equals(friendId)) {
            throw new ServiceException("Không thể gửi lời mời kết bạn với chính mình");
        }
        
        // Kiểm tra có friendship đang chờ xác nhận hoặc đã chấp nhận không
        Optional<Friendship> existingFriendship = friendshipRepository.findAnyFriendshipBetweenUsers(user, friend);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                throw new ServiceException("Đã có lời mời kết bạn với người dùng này");
            } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new ServiceException("Đã là bạn bè với người dùng này");
            } else if (friendship.getStatus() == FriendshipStatus.REJECTED) {
                // Cho phép gửi lại: cập nhật lại hướng và trạng thái
                friendship.setUser(user);
                friendship.setFriend(friend);
                friendship.setInitiator(user);
                friendship.setStatus(FriendshipStatus.PENDING);
                return friendshipRepository.save(friendship);
            }
        }
        
        Friendship friendship = Friendship.builder()
                .user(user)
                .friend(friend)
                .status(FriendshipStatus.PENDING)
                .initiator(user)
                .build();
                

        return friendshipRepository.save(friendship);
    }

    @Override
    public Friendship acceptFriendship(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ServiceException("Friendship not found"));
        
        // Kiểm tra người accept phải là người nhận lời mời (friend), không phải người gửi
        if (!friendship.getFriend().getId().equals(userId)) {
            throw new ServiceException("Bạn không có quyền chấp nhận lời mời kết bạn này");
        }
        
        // Kiểm tra trạng thái phải là PENDING
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ServiceException("Lời mời kết bạn này không còn hợp lệ");
        }
        
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }

    @Override
    public Friendship rejectFriendship(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ServiceException("Friendship not found"));
        
        // Kiểm tra người reject phải là người nhận lời mời (friend), không phải người gửi
        if (!friendship.getFriend().getId().equals(userId)) {
            throw new ServiceException("Bạn không có quyền từ chối lời mời kết bạn này");
        }
        
        // Kiểm tra trạng thái phải là PENDING
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ServiceException("Lời mời kết bạn này không còn hợp lệ");
        }
        
        friendship.setStatus(FriendshipStatus.REJECTED);
        return friendshipRepository.save(friendship);
    }

    @Override
    public List<User> getFriendsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));
        
        List<Friendship> friendships = friendshipRepository.findFriendshipsByUser(user);
        
        return friendships.stream()
                .map(friendship -> {
                    // Nếu user là người gửi, trả về friend
                    if (friendship.getUser().getId().equals(userId)) {
                        return friendship.getFriend();
                    }
                    // Nếu user là người nhận, trả về user (người gửi)
                    else {
                        return friendship.getUser();
                    }
                })
                .toList();
    }

    @Override
    public List<Friendship> getPendingFriendships(Long userId) {
       
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));
        
        return friendshipRepository.findByFriendAndStatus(user, FriendshipStatus.PENDING);
    }

    @Override
    public List<Friendship> getSentFriendships(Long userId) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));
        
        return friendshipRepository.findByInitiatorAndStatus(user, FriendshipStatus.PENDING);
    }

    @Override
    public boolean areFriends(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new ServiceException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new ServiceException("User not found"));
        
        // Check if friendship exists in either direction with ACCEPTED status
        Optional<Friendship> friendship1 = friendshipRepository.findByUserAndFriend(user1, user2);
        Optional<Friendship> friendship2 = friendshipRepository.findByUserAndFriend(user2, user1);
        
        return (friendship1.isPresent() && friendship1.get().getStatus() == FriendshipStatus.ACCEPTED) ||
               (friendship2.isPresent() && friendship2.get().getStatus() == FriendshipStatus.ACCEPTED);
    }

    @Override
    public boolean hasPendingRequest(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new ServiceException("User not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new ServiceException("User not found"));
        
        // Check if there's a pending friendship request from user1 to user2
        Optional<Friendship> friendship = friendshipRepository.findByUserAndFriend(user1, user2);
        return friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.PENDING;
    }

    @Override
    public Friendship cancelFriendship(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ServiceException("Friendship not found"));
        
        // Kiểm tra người cancel phải là người gửi lời mời
        if (!friendship.getUser().getId().equals(userId)) {
            throw new ServiceException("Bạn không có quyền hủy lời mời kết bạn này");
        }
        
        // Kiểm tra trạng thái phải là PENDING
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ServiceException("Lời mời kết bạn này không còn hợp lệ");
        }
        
        friendshipRepository.delete(friendship);
        return friendship;
    }

    @Override
    public Friendship cancelFriendshipRequest(Long friendshipId, Long userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ServiceException("Friendship not found"));
        
        // Kiểm tra người cancel phải là người gửi lời mời
        if (!friendship.getUser().getId().equals(userId)) {
            throw new ServiceException("Bạn không có quyền hủy lời mời kết bạn này");
        }

        if(friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ServiceException("Bạn không có quyền hủy lời mời kết bạn này");
        }
        
        friendshipRepository.delete(friendship);
        return friendship;
    }

}
