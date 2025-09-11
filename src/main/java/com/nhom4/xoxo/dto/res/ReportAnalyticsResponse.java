package com.nhom4.xoxo.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportAnalyticsResponse {
    private Integer totalReports;
    private Integer pendingReports;
    private Integer resolvedReports;
    private Integer rejectedReports;
    private Map<String, Integer> reportsByType; // USER, POST, COMMENT, etc.
    private Map<String, Integer> reportsByReason; // SPAM, HARASSMENT, etc.
    private Map<String, Integer> reportsByStatus;
    private Map<String, Integer> reportsByPriority;
    private Map<String, Integer> reportsThisWeek; // Last 7 days
    private Double averageResolutionTimeHours;
    private Integer reportersCount; // Unique reporters
    private LocalDateTime lastUpdated;
}







