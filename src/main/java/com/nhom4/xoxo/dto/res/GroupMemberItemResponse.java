package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberItemResponse {
    private UserResponse user;
    private GroupMembershipInfo membership;
}


