package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.PrivacyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupRequest {
    @NotBlank(message = "Group title cannot be blank")
    private String title;
    private String description;
    private String coverUrl;
    @NotNull(message = "Privacy level cannot be null")
    private PrivacyLevel privacy;
    private String rules;
    private String tags; // JSON array of tags
    private String location;
    private String website;
}
