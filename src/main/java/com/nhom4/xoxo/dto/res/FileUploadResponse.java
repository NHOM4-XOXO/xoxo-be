package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.FileType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadResponse {
    
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String thumbnailUrl;
    private FileType fileType;
    private Long fileSize;
    private String mimeType;
    private Integer duration;
    private Integer width;
    private Integer height;
    private Long chatMessageId;
    private Long uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
    private boolean encrypted;
    private String downloadUrl;
    private String previewUrl;
}
