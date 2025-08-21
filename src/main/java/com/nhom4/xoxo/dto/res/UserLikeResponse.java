package com.nhom4.xoxo.dto.res;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLikeResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String username;
    private Set<String> roles;
}