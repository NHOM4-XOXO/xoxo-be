package com.nhom4.xoxo.dto.res;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostWithMediaResponse {
    private PostItemResponse post;
    private List<MediaResponse> media;
}


