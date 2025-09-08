package com.nhom4.xoxo.service;

import java.util.List;

import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportAnalyticsResponse;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    // Basic operations
    ReportResponse createReport(ReportRequest reportRequest);
    ReportResponse getReportById(Long id);
    List<ReportResponse> getAllReports();
    ReportResponse updateReport(Long id, ReportRequest reportRequest);
    ReportResponse updateReportStatus(Long id, String status);
    void deleteReport(Long id);
    List<ReportResponse> getReportByStatus(String status);

    // Enhanced operations
    Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable);
    Page<ReportResponse> getReportsByTarget(ReportTargetType targetType, Long targetId, Pageable pageable);
    Page<ReportResponse> getReportsByReason(ReportReason reason, Pageable pageable);
    Page<ReportResponse> getReportsByPriority(Integer priority, Pageable pageable);

    // Admin operations
    ReportResponse reviewReport(Long reportId, ReportStatus status, String adminNotes, Integer priority, Long adminId);
    ReportAnalyticsResponse getReportAnalytics();
}
