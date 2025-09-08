package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.GroupMemberRequest;
import com.nhom4.xoxo.dto.res.GroupMemberResponse;
import com.nhom4.xoxo.enums.GroupMemberStatus;
import com.nhom4.xoxo.service.GroupMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/group-members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    @PostMapping("/join/{groupId}")
    public ResponseEntity<WrapRes<GroupMemberResponse>> joinGroup(@PathVariable Long groupId) {
        GroupMemberResponse response = groupMemberService.joinGroup(groupId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @DeleteMapping("/leave/{groupId}")
    public ResponseEntity<WrapRes<String>> leaveGroup(@PathVariable Long groupId) {
        groupMemberService.leaveGroup(groupId);
        return ResponseEntity.ok(WrapRes.success("Successfully left the group."));
    }

    @PutMapping("/{groupId}/members/{memberUserId}/status")
    @PreAuthorize("isAuthenticated() and @groupService.isGroupCreator(#groupId, authentication.principal.id)")
    public ResponseEntity<WrapRes<GroupMemberResponse>> updateGroupMemberStatus(
            @PathVariable Long groupId,
            @PathVariable Long memberUserId,
            @RequestBody GroupMemberRequest request) {
        GroupMemberResponse response = groupMemberService.updateGroupMemberStatus(groupId, memberUserId,
                request.getStatus());
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<WrapRes<com.nhom4.xoxo.dto.res.GroupMembersResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(required = false) GroupMemberStatus status,
            Pageable pageable) {
        var response = groupMemberService.getGroupMembersDetail(groupId, status, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }
}
