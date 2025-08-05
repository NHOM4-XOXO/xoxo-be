package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.GroupMemberStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMemberRequest {
    @NotNull(message = "Group member status cannot be null")
    private GroupMemberStatus status;
}
