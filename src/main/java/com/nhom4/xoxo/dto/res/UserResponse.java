package com.nhom4.xoxo.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.enums.GenderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<Role> roles;
    private LocalDate dateOfBirth;
    private GenderStatus gender;
    private String avatarUrl;
    private String coverUrl;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean enabled;
    private String username;
}
