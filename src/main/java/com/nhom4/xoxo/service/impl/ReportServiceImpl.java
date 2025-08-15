package com.nhom4.xoxo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.entity.Report;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.ReportStatus;
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
        report.setReproter(reporter);
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

    private ReportResponse toReportResponse(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReproter().getId())
                .reporterName(r.getReproter().getUsername())
                .reporterEmail(r.getReproter().getEmail())
                .reportTargetType(r.getReportTargetType())
                .reportTargetId(r.getReportTargetId())
                .reportReason(r.getReportReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
