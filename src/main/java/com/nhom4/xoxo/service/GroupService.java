package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.req.CreateGroupRequest;
import com.nhom4.xoxo.dto.req.UpdateGroupRequest;
import com.nhom4.xoxo.dto.res.GroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupService {
    GroupResponse createGroup(CreateGroupRequest request, Long userId);
    GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, Long userId);
    void deleteGroup(Long groupId, Long userId);
    GroupResponse getGroupById(Long groupId);
    Page<GroupResponse> getAllGroups(Pageable pageable);
    Page<GroupResponse> getGroupsByCreator(Long creatorId, Pageable pageable);
    Page<GroupResponse> searchGroups(String keyword, Pageable pageable);
}
