package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReportReviewRequest {
    @NotNull(message = "Status cannot be null")
    private ReportStatus status;
    private String adminNotes;
    private Integer priority; // 1=Low, 2=Medium, 3=High, 4=Critical
}












