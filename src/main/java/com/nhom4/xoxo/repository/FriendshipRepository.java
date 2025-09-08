package com.nhom4.xoxo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Friendship;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.FriendshipStatus;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

       // Tìm tất cả friendship của 1 user (cả 2 vai trò)
       @Query("SELECT f FROM Friendship f WHERE (f.user = :user OR f.friend = :user) AND f.status = :status")
       List<Friendship> findByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);

       // Tìm tất cả bạn bè của 1 user
       @Query("SELECT f FROM Friendship f WHERE " +
                     "((f.user = :user AND f.friend != :user) OR (f.friend = :user AND f.user != :user)) " +
                     "AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.ACCEPTED")
       List<Friendship> findFriendshipsByUser(@Param("user") User user);

       // Tìm lời mời kết bạn đang chờ xác nhận
       @Query("SELECT f FROM Friendship f WHERE f.friend = :user AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.PENDING")
       List<Friendship> findPendingRequestsForUser(@Param("user") User user);

       // Tìm lời mời kết bạn đã gửi
       @Query("SELECT f FROM Friendship f WHERE f.initiator = :user AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.PENDING")
       List<Friendship> findSentRequestsByUser(@Param("user") User user);

       // Kiểm tra 2 user có phải bạn bè không
       @Query("SELECT f FROM Friendship f WHERE " +
                     "((f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)) " +
                     "AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.ACCEPTED")
       Optional<Friendship> findFriendshipBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

       // Kiểm tra có lời mời kết bạn nào giữa 2 user không
       @Query("SELECT f FROM Friendship f WHERE " +
                     "((f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1))")
       Optional<Friendship> findAnyFriendshipBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

       // Đếm số bạn bè của 1 user
       @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
                     "((f.user = :user AND f.friend != :user) OR (f.friend = :user AND f.user != :user)) " +
                     "AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.ACCEPTED")
       long countFriendsByUser(@Param("user") User user);

       List<Friendship> findByFriendAndStatus(User user, FriendshipStatus status);

       List<Friendship> findByInitiatorAndStatus(User user, FriendshipStatus status);
       
       // Find specific friendship from user1 to user2
       @Query("SELECT f FROM Friendship f WHERE f.user = :user AND f.friend = :friend")
       Optional<Friendship> findByUserAndFriend(@Param("user") User user, @Param("friend") User friend);
}
