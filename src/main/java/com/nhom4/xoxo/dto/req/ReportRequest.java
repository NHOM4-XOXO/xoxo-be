package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.ReportTargetType;

import lombok.Data;

@Data
public class ReportRequest {
    private Long reporterId;
    private ReportTargetType reportTargetType;
    private Long reportTargetId;
    private String reportReason;
}
