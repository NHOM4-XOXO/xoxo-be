package com.nhom4.xoxo.dto.res;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private long id;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private ReportTargetType reportTargetType;
    private Long reportTargetId;
    private String reportReason;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
