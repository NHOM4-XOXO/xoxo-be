package com.nhom4.xoxo.dto.res;

import com.nhom4.xoxo.enums.GroupMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponse {
    private Long groupId;
    private Long userId;
    private GroupResponse group;
    private UserResponse user;
    private GroupMemberStatus status;
}
