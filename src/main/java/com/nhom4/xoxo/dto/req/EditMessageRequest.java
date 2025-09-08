package com.nhom4.xoxo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditMessageRequest {
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
    private String content;
    
    private String mediaUrl;
    private String mediaType;
}


