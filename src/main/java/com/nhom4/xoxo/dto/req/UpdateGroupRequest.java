package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.PrivacyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGroupRequest {
    private String title;
    private String description;
    private String coverUrl;
    private PrivacyLevel privacy;
}
