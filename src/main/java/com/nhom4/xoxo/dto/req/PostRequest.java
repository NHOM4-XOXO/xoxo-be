package com.nhom4.xoxo.dto.req;

import java.util.List;

import com.nhom4.xoxo.enums.PostStatus;
import com.nhom4.xoxo.enums.PostType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostRequest {
    
    @NotBlank(message = "Content không được để trống")
    @Size(max = 10000, message = "Content không được quá 10000 ký tự")
    private String content;
    
    @NotNull(message = "Status không được null")
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;
    
    @NotNull(message = "Type không được null")
    @Builder.Default
    private PostType type = PostType.USER_POST;
    
    private Long parentPostId; // Cho reply/share
    
    private String location; // Vị trí đăng bài
    
    @Size(max = 500, message = "Hashtags không được quá 500 ký tự")
    private String hashtags; // Hashtags, phân cách bằng dấu phẩy
    
    @NotNull(message = "isPublic không được null")
    @Builder.Default
    private Boolean isPublic = true;
    
    @NotNull(message = "allowComments không được null")
    @Builder.Default
    private Boolean allowComments = true;
    
    @NotNull(message = "allowLikes không được null")
    @Builder.Default
    private Boolean allowLikes = true;
    
    @NotNull(message = "allowShares không được null")
    @Builder.Default
    private Boolean allowShares = true;
    
    private List<Long> mediaIds; // Danh sách media IDs để thêm vào post
}
