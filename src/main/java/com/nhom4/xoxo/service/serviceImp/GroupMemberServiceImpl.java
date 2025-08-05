package com.nhom4.xoxo.service.serviceImp;

import com.nhom4.xoxo.dto.res.GroupMemberResponse;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.GroupMember;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.GroupMemberStatus;
import com.nhom4.xoxo.enums.PrivacyLevel;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.repository.GroupMemberRepository;
import com.nhom4.xoxo.repository.GroupRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.GroupMemberService;
import com.nhom4.xoxo.service.UserService;
import com.nhom4.xoxo.untils.MapperUntils;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email);
        return user;
    }

    @Override
    @Transactional
    public GroupMemberResponse joinGroup(Long groupId) {
        User currentUser = getCurrentUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        GroupMember.GroupMemberId memberId = new GroupMember.GroupMemberId(groupId, currentUser.getId());
        Optional<GroupMember> existingMember = groupMemberRepository.findById(memberId);

        if (existingMember.isPresent()) {
            GroupMember member = existingMember.get();
            if (member.getStatus() == GroupMemberStatus.ACCEPTED) {
                throw new ServiceException("You are already a member of this group.");
            } else if (member.getStatus() == GroupMemberStatus.PENDING) {
                throw new ServiceException("Your request to join this group is already pending.");
            } else if (member.getStatus() == GroupMemberStatus.BLOCKED) {
                throw new ServiceException("You are blocked from joining this group.");
            }
        }

        GroupMemberStatus status = (group.getPrivacy() == PrivacyLevel.PUBLIC) ? GroupMemberStatus.ACCEPTED
                : GroupMemberStatus.PENDING;

        GroupMember newMember = GroupMember.builder()
                .id(memberId)
                .group(group)
                .user(currentUser)
                .status(status)
                .build();

        GroupMember savedMember = groupMemberRepository.save(newMember);
        return MapperUntils.mapObject(savedMember, GroupMemberResponse.class);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId) {
        User currentUser = getCurrentUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        GroupMember.GroupMemberId memberId = new GroupMember.GroupMemberId(groupId, currentUser.getId());
        GroupMember groupMember = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new ServiceException("You are not a member of this group."));

        if (group.getCreator().getId().equals(currentUser.getId())) {
            throw new ServiceException("Group creator cannot leave their own group. Please delete the group instead.");
        }

        groupMemberRepository.delete(groupMember);
    }

    @Override
    @Transactional
    public GroupMemberResponse updateGroupMemberStatus(Long groupId, Long memberUserId, GroupMemberStatus newStatus) {
        User currentUser = getCurrentUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        if (!group.getCreator().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the group creator can update member status.");
        }

        User memberUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> new NotFoundException("Member user not found with ID: " + memberUserId));

        GroupMember.GroupMemberId memberId = new GroupMember.GroupMemberId(groupId, memberUserId);
        GroupMember groupMember = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("User is not a member of this group."));

        // Prevent creator from changing their own status
        if (group.getCreator().getId().equals(memberUser.getId())) {
            throw new ServiceException("Cannot change the status of the group creator.");
        }

        // Validate status transitions
        if (groupMember.getStatus() == GroupMemberStatus.BLOCKED && newStatus != GroupMemberStatus.BLOCKED) {
            throw new ServiceException("Cannot unblock a user directly. User must re-request to join.");
        }
        if (newStatus == GroupMemberStatus.PENDING) {
            throw new ServiceException("Cannot set status to PENDING manually. Users request to join as PENDING.");
        }

        groupMember.setStatus(newStatus);

        GroupMember updatedMember = groupMemberRepository.save(groupMember);

        return MapperUntils.mapObject(updatedMember, GroupMemberResponse.class);
    }

    @Override
    @Transactional
    public Page<GroupMemberResponse> getGroupMembers(Long groupId, GroupMemberStatus status, Pageable pageable) {
        User currentUser = getCurrentUser();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        // Check if current user is a member or creator to view members of a private
        // group
        boolean isMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .map(gm -> gm.getStatus() == GroupMemberStatus.ACCEPTED)
                .orElse(false);
        boolean isCreator = group.getCreator().getId().equals(currentUser.getId());

        if (group.getPrivacy() == PrivacyLevel.PRIVATE && !isMember && !isCreator) {
            throw new ForbiddenException("You do not have permission to view members of this private group.");
        }

        Page<GroupMember> members;
        if (status != null) {
            members = groupMemberRepository.findByGroupAndStatus(group, status, pageable);
        } else {
            members = groupMemberRepository.findByGroup(group, pageable);
        }

        return members.map(member -> MapperUntils.mapObject(member, GroupMemberResponse.class));
    }
}
