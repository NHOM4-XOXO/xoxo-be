package com.nhom4.xoxo.service;

import java.util.List;

import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.enums.ReportStatus;

public interface ReportService {
    ReportResponse createReport(ReportRequest reportRequest);

    ReportResponse getReportById(Long id);

    List<ReportResponse> getAllReports();

    ReportResponse updateReportStatus(Long id, String status);

    void deleteReport(Long id);

    List<ReportResponse> getReportByStatus(String status);

}
