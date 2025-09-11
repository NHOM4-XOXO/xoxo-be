package com.nhom4.xoxo.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.entity.Report;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.repository.ReportRepository;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.service.ReportService;
import com.nhom4.xoxo.untils.MapperUntils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Override
    public ReportResponse createReport(ReportRequest reportRequest) {
        Report report = MapperUntils.mapObject(reportRequest, Report.class);
        User reporter = userRepository.findById(reportRequest.getReporterId())
                .orElseThrow(() -> new NotFoundException("Reporter not found"));
        report.setReporter(reporter);
        report = reportRepository.save(report);

        return toReportResponse(report);
    }

    @Override
    public ReportResponse getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        return toReportResponse(report);
    }

    @Override
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::toReportResponse)
                .toList(); // nếu dùng JDK 8 thì thay bằng .collect(Collectors.toList())
    }

    @Override
    public ReportResponse updateReport(Long id, ReportRequest reportRequest) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        // Only allow updates if report is still PENDING
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ServiceException("Cannot update report that has already been reviewed");
        }

        // Update allowed fields
        if (reportRequest.getReportReason() != null) {
            report.setReportReason(reportRequest.getReportReason());
        }
        if (reportRequest.getAdditionalInfo() != null) {
            report.setAdditionalInfo(reportRequest.getAdditionalInfo());
        }
        if (reportRequest.getPriority() != null) {
            report.setPriority(reportRequest.getPriority());
        }

        report = reportRepository.save(report);
        return toReportResponse(report);
    }

    @Override
    public ReportResponse updateReportStatus(Long id, String status) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        try {
            report.setStatus(ReportStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Invalid report status: " + status);
        }

        report = reportRepository.save(report);
        return toReportResponse(report);
    }

    @Override
    public void deleteReport(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new NotFoundException("Report not found");
        }
        reportRepository.deleteById(id);
    }

    @Override
    public List<ReportResponse> getReportByStatus(String status) {
        ReportStatus reportStatus;
        try {
            reportStatus = ReportStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Invalid status value" + status);
        }
        return reportRepository.findByStatus(reportStatus).stream()
                .map(this::toReportResponse)
                .toList();
    }

    // Enhanced operations
    @Override
    public Page<ReportResponse> getReportsByUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return reportRepository.findByReporterOrderByCreatedAtDesc(user, pageable)
                .map(this::toReportResponse);
    }

    @Override
    public Page<ReportResponse> getReportsByTarget(ReportTargetType targetType, Long targetId, Pageable pageable) {
        return reportRepository.findByReportTargetTypeAndReportTargetIdOrderByCreatedAtDesc(targetType, targetId, pageable)
                .map(this::toReportResponse);
    }

    @Override
    public Page<ReportResponse> getReportsByReason(ReportReason reason, Pageable pageable) {
        return reportRepository.findByReportReasonOrderByCreatedAtDesc(reason, pageable)
                .map(this::toReportResponse);
    }

    @Override
    public Page<ReportResponse> getReportsByPriority(Integer priority, Pageable pageable) {
        return reportRepository.findByPriorityOrderByCreatedAtDesc(priority, pageable)
                .map(this::toReportResponse);
    }

    // Admin operations
    @Override
    public ReportResponse reviewReport(Long reportId, ReportStatus status, String adminNotes, Integer priority, Long adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        report.setStatus(status);
        report.setAdminNotes(adminNotes);
        report.setReviewedBy(admin);
        report.setReviewedAt(java.time.LocalDateTime.now());
        if (priority != null) {
            report.setPriority(priority);
        }

        report = reportRepository.save(report);
        return toReportResponse(report);
    }

    @Override
    public com.nhom4.xoxo.dto.res.ReportAnalyticsResponse getReportAnalytics() {
        long totalReports = reportRepository.count();
        
        // Count by status
        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING);
        long resolvedReports = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedReports = reportRepository.countByStatus(ReportStatus.REJECTED);
        
        // Aggregate by target type
        java.util.Map<String, Integer> reportsByType = new java.util.HashMap<>();
        List<Object[]> typeData = reportRepository.countByTargetType();
        for (Object[] row : typeData) {
            reportsByType.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        
        // Aggregate by reason
        java.util.Map<String, Integer> reportsByReason = new java.util.HashMap<>();
        List<Object[]> reasonData = reportRepository.countByReason();
        for (Object[] row : reasonData) {
            reportsByReason.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        
        // Aggregate by status
        java.util.Map<String, Integer> reportsByStatus = java.util.Map.of(
            "PENDING", (int) pendingReports,
            "RESOLVED", (int) resolvedReports,
            "REJECTED", (int) rejectedReports,
            "IN_PROGRESS", (int) reportRepository.countByStatus(ReportStatus.IN_PROGRESS),
            "CLOSED", (int) reportRepository.countByStatus(ReportStatus.CLOSED),
            "ESCALATED", (int) reportRepository.countByStatus(ReportStatus.ESCALATED)
        );
        
        // Aggregate by priority
        java.util.Map<String, Integer> reportsByPriority = new java.util.HashMap<>();
        List<Object[]> priorityData = reportRepository.countByPriority();
        for (Object[] row : priorityData) {
            String priorityLabel = getPriorityLabel(((Number) row[0]).intValue());
            reportsByPriority.put(priorityLabel, ((Number) row[1]).intValue());
        }
        
        // Reports this week
        java.util.Map<String, Integer> reportsThisWeek = new java.util.HashMap<>();
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        List<Object[]> weekData = reportRepository.countReportsThisWeek(sevenDaysAgo);
        for (Object[] row : weekData) {
            reportsThisWeek.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        
        // Average resolution time
        Double avgResolutionTime = reportRepository.getAverageResolutionTimeInHours();
        if (avgResolutionTime == null) avgResolutionTime = 0.0;
        
        // Count distinct reporters
        long reportersCount = reportRepository.countDistinctReporters();
        
        return com.nhom4.xoxo.dto.res.ReportAnalyticsResponse.builder()
                .totalReports((int) totalReports)
                .pendingReports((int) pendingReports)
                .resolvedReports((int) resolvedReports)
                .rejectedReports((int) rejectedReports)
                .reportsByType(reportsByType)
                .reportsByReason(reportsByReason)
                .reportsByStatus(reportsByStatus)
                .reportsByPriority(reportsByPriority)
                .reportsThisWeek(reportsThisWeek)
                .averageResolutionTimeHours(avgResolutionTime)
                .reportersCount((int) reportersCount)
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
    }
    
    private String getPriorityLabel(int priority) {
        switch (priority) {
            case 1: return "Low";
            case 2: return "Medium";
            case 3: return "High";
            case 4: return "Critical";
            default: return "Unknown";
        }
    }

    private ReportResponse toReportResponse(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterName(r.getReporter().getUsername())
                .reporterEmail(r.getReporter().getEmail())
                .reportTargetType(r.getReportTargetType())
                .reportTargetId(r.getReportTargetId())
                .reportReason(r.getReportReason())
                .additionalInfo(r.getAdditionalInfo())
                .status(r.getStatus())
                .reviewedById(r.getReviewedBy() != null ? r.getReviewedBy().getId() : null)
                .reviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getUsername() : null)
                .reviewedAt(r.getReviewedAt())
                .adminNotes(r.getAdminNotes())
                .isAnonymous(r.getIsAnonymous())
                .priority(r.getPriority())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
