package com.nhom4.xoxo.dto.req;

import java.util.List;

import com.nhom4.xoxo.enums.PrivacyLevel;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryRequest {

    @Size(max = 1000, message = "Story content cannot exceed 1000 characters")
    private String content;

    @Builder.Default
    private PrivacyLevel privacy = PrivacyLevel.PUBLIC;

    private List<Long> mediaIds;
}