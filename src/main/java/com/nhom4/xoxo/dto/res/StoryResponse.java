package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;
import java.util.List;

import com.nhom4.xoxo.enums.PrivacyLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryResponse {

    private Long id;
    private String content;
    private PrivacyLevel privacy;
    private UserResponse user;
    private List<MediaResponse> media;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}