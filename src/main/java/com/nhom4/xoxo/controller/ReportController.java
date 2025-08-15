package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.dto.req.ReportRequest;
import com.nhom4.xoxo.dto.res.ReportResponse;
import com.nhom4.xoxo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<WrapRes<ReportResponse>> createReport(
            @RequestBody ReportRequest request) {
        ReportResponse response = reportService.createReport(request);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WrapRes<ReportResponse>> getReportById(@PathVariable Long id) {
        ReportResponse response = reportService.getReportById(id);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @GetMapping
    public ResponseEntity<WrapRes<List<ReportResponse>>> getAllReports() {
        List<ReportResponse> responses = reportService.getAllReports();
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<WrapRes<List<ReportResponse>>> getReportByStatus(@PathVariable String status) {
        List<ReportResponse> responses = reportService.getReportByStatus(status);
        return ResponseEntity.ok(WrapRes.success(responses));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<WrapRes<ReportResponse>> updateReportStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        ReportResponse response = reportService.updateReportStatus(id, status);
        return ResponseEntity.ok(WrapRes.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WrapRes<Void>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(WrapRes.success(null));
    }

}
