package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.CreateGroupRequest;
import com.nhom4.xoxo.dto.req.UpdateGroupRequest;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.GroupMemberService;
import com.nhom4.xoxo.service.GroupService;
import com.nhom4.xoxo.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Group Management", description = "APIs for managing groups in social media platform")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final GroupMemberService groupMemberService;

    public GroupController(GroupService groupService, UserService userService, GroupMemberService groupMemberService) {
        this.groupService = groupService;
        this.userService = userService;
        this.groupMemberService = groupMemberService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return user;
    }

    @Operation(summary = "Tạo group mới", description = "Tạo một group mới với thông tin được cung cấp")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tạo group thành công",
            content = @Content(schema = @Schema(implementation = GroupResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping
    public ResponseEntity<WrapRes<GroupResponse>> createGroup(
            @Parameter(description = "Thông tin group cần tạo", required = true)
            @Valid @RequestBody CreateGroupRequest request) {

        GroupResponse response = groupService.createGroup(request, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<WrapRes<GroupResponse>> updateGroup(@PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupResponse response = groupService.updateGroup(groupId, request, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<WrapRes<Void>> deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId, getCurrentUser().getId());
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<WrapRes<GroupResponse>> getGroupById(@PathVariable Long groupId) {
        GroupResponse response = groupService.getGroupById(groupId);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.getAllGroups(pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/my-groups")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getMyGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.getGroupsByCreator(getCurrentUser().getId(), pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getGroupsByCreator(
            @PathVariable Long creatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.getGroupsByCreator(creatorId, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> searchGroups(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.searchGroups(keyword, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/joined")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getJoinedGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.getJoinedGroups(getCurrentUser().getId(), pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/suggested")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getSuggestedGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupResponse> response = groupService.getSuggestedGroups(getCurrentUser().getId(), pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/popular")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getPopularGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupResponse> response = groupService.getPopularGroups(pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/nearby")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getNearbyGroups(
            @RequestParam String location,
            @RequestParam(defaultValue = "50") double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupResponse> response = groupService.getNearbyGroups(location, radiusKm, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<WrapRes<Page<GroupResponse>>> getGroupsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "memberCount,desc") String[] sort) {
        Sort sorting = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<GroupResponse> response = groupService.getGroupsByCategory(category, pageable);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @PostMapping("/{groupId}/report")
    public ResponseEntity<WrapRes<Void>> reportGroup(
            @PathVariable Long groupId,
            @RequestParam String reason,
            @RequestParam(required = false) String additionalInfo) {
        groupService.reportGroup(groupId, getCurrentUser().getId(), reason, additionalInfo);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<WrapRes<Void>> joinGroup(@PathVariable Long groupId) {
        groupMemberService.joinGroup(groupId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<WrapRes<Void>> leaveGroup(@PathVariable Long groupId) {
        groupMemberService.leaveGroup(groupId);
        return ResponseEntity.ok(WrapRes.success(null));
    }

    @GetMapping("/{groupId}/join-status")
    public ResponseEntity<WrapRes<String>> getJoinStatus(@PathVariable Long groupId) {
        // Check if current user has joined this group
        try {
            var memberResponse = groupMemberService.getGroupMembers(groupId, null, PageRequest.of(0, 1));
            return ResponseEntity.ok(WrapRes.success("JOINED"));
        } catch (Exception e) {
            return ResponseEntity.ok(WrapRes.success("NOT_JOINED"));
        }
    }
}
