package com.nhom4.xoxo.dto.res;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembersResponse {
    private GroupResponse group;
    private Page<GroupMemberItemResponse> members;
}


