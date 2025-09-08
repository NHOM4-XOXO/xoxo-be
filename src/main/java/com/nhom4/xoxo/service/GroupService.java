package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.req.CreateGroupRequest;
import com.nhom4.xoxo.dto.req.UpdateGroupRequest;
import com.nhom4.xoxo.dto.res.GroupAnalyticsResponse;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.enums.GroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupService {
    // Basic CRUD operations
    GroupResponse createGroup(CreateGroupRequest request, Long userId);
    GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, Long userId);
    void deleteGroup(Long groupId, Long userId);
    GroupResponse getGroupById(Long groupId);
    Page<GroupResponse> getAllGroups(Pageable pageable);
    Page<GroupResponse> getGroupsByCreator(Long creatorId, Pageable pageable);
    Page<GroupResponse> searchGroups(String keyword, Pageable pageable);

    // Enhanced Facebook-like features
    Page<GroupResponse> getJoinedGroups(Long userId, Pageable pageable);
    Page<GroupResponse> getSuggestedGroups(Long userId, Pageable pageable);
    Page<GroupResponse> getPopularGroups(Pageable pageable);
    Page<GroupResponse> getNearbyGroups(String location, double radiusKm, Pageable pageable);
    Page<GroupResponse> getGroupsByCategory(String category, Pageable pageable);
    void reportGroup(Long groupId, Long reporterId, String reason, String additionalInfo);

    // Admin operations
    List<GroupResponse> getAllGroupsForAdmin();
    GroupResponse updateGroupStatus(Long groupId, GroupStatus status, String adminNotes, Long adminId);
    void adminDeleteGroup(Long groupId);
    GroupAnalyticsResponse getGroupAnalytics(Long groupId);
}
