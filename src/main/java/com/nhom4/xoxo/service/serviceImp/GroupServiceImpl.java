package com.nhom4.xoxo.service.serviceImp;

import com.nhom4.xoxo.dto.req.CreateGroupRequest;
import com.nhom4.xoxo.dto.req.UpdateGroupRequest;
import com.nhom4.xoxo.dto.res.GroupResponse;
import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.GroupMember;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.GroupMemberStatus;
import com.nhom4.xoxo.exception.ForbiddenException;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.repository.GroupMemberRepository;
import com.nhom4.xoxo.repository.GroupRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.GroupService;
import com.nhom4.xoxo.untils.MapperUntils;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public GroupServiceImpl(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
            UserRepository userRepository, ModelMapper modelMapper) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Group group = modelMapper.map(request, Group.class);
        group.setCreator(creator);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        Group savedGroup = groupRepository.save(group);

        // Automatically add the creator as an accepted member
        GroupMember creatorMember = GroupMember.builder()
                .id(new GroupMember.GroupMemberId(savedGroup.getId(), creator.getId()))
                .group(savedGroup)
                .user(creator)
                .status(GroupMemberStatus.ACCEPTED)
                .build();
        groupMemberRepository.save(creatorMember);

        return MapperUntils.mapObject(savedGroup, GroupResponse.class);
    }

    @Override
    @Transactional
    public GroupResponse updateGroup(Long groupId, UpdateGroupRequest request, Long userId) {
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        if (!existingGroup.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to update this group.");
        }

        if (request.getTitle() != null) {
            existingGroup.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            existingGroup.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            existingGroup.setCoverUrl(request.getCoverUrl());
        }
        if (request.getPrivacy() != null) {
            existingGroup.setPrivacy(request.getPrivacy());
        }
        existingGroup.setUpdatedAt(LocalDateTime.now());

        Group updatedGroup = groupRepository.save(existingGroup);
        return MapperUntils.mapObject(updatedGroup, GroupResponse.class);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));

        if (!existingGroup.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to delete this group.");
        }
        groupRepository.delete(existingGroup);
    }

    @Override
    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with id: " + groupId));
        return MapperUntils.mapObject(group, GroupResponse.class);
    }

    @Override
    public Page<GroupResponse> getAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable)
                .map(group -> MapperUntils.mapObject(group, GroupResponse.class));
    }

    @Override
    public Page<GroupResponse> getGroupsByCreator(Long creatorId, Pageable pageable) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Creator user not found with id: " + creatorId));
        return groupRepository.findByCreator(creator, pageable)
                .map(group -> MapperUntils.mapObject(group, GroupResponse.class));
    }

    @Override
    public Page<GroupResponse> searchGroups(String keyword, Pageable pageable) {
        return groupRepository.findByTitle(keyword, pageable)
                .map(group -> MapperUntils.mapObject(group, GroupResponse.class));
    }
}
