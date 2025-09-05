package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.PostStatus;

import lombok.Data;

@Data
public class TogglePostStatusRequest {
    private PostStatus status;
}
