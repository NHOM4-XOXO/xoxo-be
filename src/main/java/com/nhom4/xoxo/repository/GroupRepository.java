package com.nhom4.xoxo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.GroupStatus;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    Page<Group> findByCreator(User creator, Pageable pageable);
    
    @Query("SELECT g FROM Group g WHERE g.title LIKE %:keyword% OR g.description LIKE %:keyword%")
    Page<Group> findByTitleOrDescription(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT g FROM Group g WHERE g.title LIKE %:title%")
    Page<Group> findByTitle(String title, Pageable pageable);

    // Enhanced queries for Facebook-like features
    @Query("SELECT g FROM Group g JOIN GroupMember gm ON g.id = gm.group.id " +
           "WHERE gm.user.id = :userId AND gm.status = com.nhom4.xoxo.enums.GroupMemberStatus.ACCEPTED AND g.status = com.nhom4.xoxo.enums.GroupStatus.ACTIVE")
    Page<Group> findJoinedGroupsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.status = com.nhom4.xoxo.enums.GroupStatus.ACTIVE ORDER BY g.memberCount DESC")
    Page<Group> findPopularGroups(Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.status = com.nhom4.xoxo.enums.GroupStatus.ACTIVE AND g.location LIKE %:location%")
    Page<Group> findByLocationContaining(@Param("location") String location, Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.status = com.nhom4.xoxo.enums.GroupStatus.ACTIVE AND g.tags LIKE %:category%")
    Page<Group> findByCategory(@Param("category") String category, Pageable pageable);

    // Admin queries
    List<Group> findByStatus(GroupStatus status);
    
    @Query("SELECT COUNT(g) FROM Group g WHERE g.status = :status")
    long countByStatus(@Param("status") GroupStatus status);

    // Analytics queries
    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.status = com.nhom4.xoxo.enums.GroupMemberStatus.ACCEPTED")
    int countMembersByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.status = com.nhom4.xoxo.enums.GroupMemberStatus.ACCEPTED " +
           "AND gm.createdAt >= :startDate")
    int countNewMembersThisMonth(@Param("groupId") Long groupId, @Param("startDate") java.time.LocalDateTime startDate);

    // Suggested groups - groups that user's friends have joined but user hasn't
    @Query("SELECT DISTINCT g FROM Group g " +
           "JOIN GroupMember gm ON g.id = gm.group.id " +
           "JOIN Friendship f ON gm.user.id = f.friend.id " +
           "WHERE f.user.id = :userId AND f.status = com.nhom4.xoxo.enums.FriendshipStatus.ACCEPTED " +
           "AND g.status = com.nhom4.xoxo.enums.GroupStatus.ACTIVE " +
           "AND g.id NOT IN (" +
           "  SELECT gm2.group.id FROM GroupMember gm2 " +
           "  WHERE gm2.user.id = :userId AND gm2.status = com.nhom4.xoxo.enums.GroupMemberStatus.ACCEPTED" +
           ") " +
           "ORDER BY g.memberCount DESC")
    Page<Group> findSuggestedGroupsForUser(@Param("userId") Long userId, Pageable pageable);
}
