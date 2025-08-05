package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.CreateGroupRequest;
import com.nhom4.xoxo.dto.req.UpdateGroupRequest;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.GroupService;
import com.nhom4.xoxo.service.UserService;

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
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return user;
    }

    @PostMapping
    public ResponseEntity<WrapRes<GroupResponse>> createGroup(@Valid @RequestBody CreateGroupRequest request) {

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

    @GetMapping("/my-groups/{creatorId}")
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
}
