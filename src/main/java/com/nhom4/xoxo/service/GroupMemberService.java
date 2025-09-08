package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.res.GroupMemberResponse;
import com.nhom4.xoxo.dto.res.GroupMembersResponse;
import com.nhom4.xoxo.enums.GroupMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupMemberService {
    GroupMemberResponse joinGroup(Long groupId);

    void leaveGroup(Long groupId);

    GroupMemberResponse updateGroupMemberStatus(Long groupId, Long memberUserId, GroupMemberStatus newStatus);

    Page<GroupMemberResponse> getGroupMembers(Long groupId, GroupMemberStatus status, Pageable pageable);

    GroupMembersResponse getGroupMembersDetail(Long groupId, GroupMemberStatus status, Pageable pageable);
}
