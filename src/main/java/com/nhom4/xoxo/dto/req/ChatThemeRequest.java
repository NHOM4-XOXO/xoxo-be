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
public class ChatThemeRequest {
    @NotBlank(message = "Theme name cannot be blank")
    @Pattern(regexp = "^(default|dark|blue|pink|purple|green|orange|red)$", 
             message = "Theme must be one of: default, dark, blue, pink, purple, green, orange, red")
    private String theme;
    
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", 
             message = "Message color must be a valid hex color")
    private String messageColor;
    
    @Pattern(regexp = "^[\\p{So}\\p{Cn}]$", 
             message = "Emoji must be a valid emoji character")
    private String emoji;
}
