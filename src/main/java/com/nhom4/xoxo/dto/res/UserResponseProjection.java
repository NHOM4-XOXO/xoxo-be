package com.nhom4.xoxo.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;


import com.nhom4.xoxo.entity.Role;
import com.nhom4.xoxo.enums.GenderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface UserResponseProjection {
    Long getId();
    String getEmail();
    String getFirstName();
    String getLastName();
    String getRoles();
    LocalDate getDateOfBirth();
    GenderStatus getGender();
    String getAvatarUrl();
    String getCoverUrl();
    String getBio();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    boolean getEnabled();
}