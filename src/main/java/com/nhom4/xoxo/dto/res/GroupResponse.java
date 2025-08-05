package com.nhom4.xoxo.dto.res;

import com.nhom4.xoxo.enums.PrivacyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private UserResponse creator;
    private PrivacyLevel privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
