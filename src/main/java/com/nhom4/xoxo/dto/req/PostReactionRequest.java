package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.PostReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostReactionRequest {
    @NotNull(message = "Post ID cannot be null")
    private Long postId;
    
    @NotNull(message = "Reaction type cannot be null")
    private PostReactionType reactionType;
}





