package com.nhom4.xoxo.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.GenderStatus;

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
    String getUsername();
}