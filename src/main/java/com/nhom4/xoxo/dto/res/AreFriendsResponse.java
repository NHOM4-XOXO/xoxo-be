package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreFriendsResponse {
    private boolean areFriends;
    private long friendshipId;
}
