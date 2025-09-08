package com.nhom4.xoxo.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReactionRequest {
    @NotBlank(message = "Message ID cannot be blank")
    private String messageId;
    
    @NotBlank(message = "Reaction cannot be blank")
    @Pattern(regexp = "^[\\p{So}\\p{Cn}]+$", 
             message = "Reaction must be a valid emoji")
    private String reaction;
}
