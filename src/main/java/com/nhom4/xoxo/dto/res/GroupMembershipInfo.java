package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.GroupMemberStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembershipInfo {
    private GroupMemberStatus status;
    private String role;              // Optional: default "MEMBER" for now
    private LocalDateTime joinedAt;   // Mapped from GroupMember.createdAt
}


