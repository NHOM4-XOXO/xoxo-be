package com.nhom4.xoxo.repository;

import com.nhom4.xoxo.entity.Group;
import com.nhom4.xoxo.entity.GroupMember;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.GroupMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMember.GroupMemberId> {
    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    Page<GroupMember> findByGroupAndStatus(Group group, GroupMemberStatus status, Pageable pageable);

    Page<GroupMember> findByUserAndStatus(User user, GroupMemberStatus status, Pageable pageable);

    Page<GroupMember> findByGroup(Group group, Pageable pageable);
}
